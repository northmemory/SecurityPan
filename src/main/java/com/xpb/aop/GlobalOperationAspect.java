package com.xpb.aop;


import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.utils.RegexUtil;
import com.xpb.utils.ResponseResult;
import com.xpb.utils.exceptions.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class GlobalOperationAspect {
    @Pointcut("@annotation(interceptor)")
    private void requestInterceptor(GlobalInterceptor interceptor){
    }
    @Around("requestInterceptor(interceptor)")
    public Object AroundAdvice(ProceedingJoinPoint pointcut, GlobalInterceptor interceptor) throws Throwable {

        Object[] args = pointcut.getArgs();
        Object target = pointcut.getTarget();
        /*String methodName=pointcut.getSignature().getName();
        Class<?>[] parameterTypes = ((MethodSignature) pointcut.getSignature()).getMethod().getParameterTypes();
        Method method= target.getClass().getMethod(methodName,parameterTypes);*/
        Method method = ((MethodSignature) pointcut.getSignature()).getMethod();

        if (interceptor.checkParams()){
            try {
                validateParams(method,args);
            } catch (BusinessException e) {
                return new ResponseResult(500,e.getMessage());
            }
        }
        return pointcut.proceed();
    }

    public void validateParams(Method m,Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter=parameters[i];
            String parameterName=parameter.getAnnotation(RequestParam.class)==null? parameter.getName() : parameter.getAnnotation(RequestParam.class).value();
            Object value=arguments[i];
            VerifyParam verifyParam=parameter.getAnnotation(VerifyParam.class);
            if (verifyParam==null) continue;
            String typeName = parameter.getParameterizedType().getTypeName();
            if (typeName.equals("java.lang.String")||typeName.equals("java.lang.Integer")||typeName.equals("java.lang.Long"))
                checkValue(value, verifyParam, parameterName);
            else
                checkObjValue(parameter, value, parameterName);
        }
    }

    public void checkValue(Object value, VerifyParam verifyParam, String parameterName) throws BusinessException {
        Boolean isEempty= value==null || value.toString().equals("");
        Integer length=value==null ? 0 : value.toString().length();
        //校验空值
        if (isEempty && verifyParam.required())
            throw new BusinessException(600,parameterName+"参数为空");
        //检验长度
        if (!isEempty && (verifyParam.max()!=-1 && verifyParam.max()<length || verifyParam.min()!=-1 && verifyParam.min()>length))
            throw new BusinessException(600,parameterName+"参数过长");
        //校验正则
        if ((!isEempty && verifyParam.regexVerify().getRegex()!="") && !RegexUtil.verify(value.toString(),verifyParam.regexVerify().getRegex()))
            throw new BusinessException(600,parameterName+"不满足要求的格式");
    }

    public void checkObjValue(Parameter parameter, Object value, String parameterName) throws BusinessException {
        String typeName=parameter.getParameterizedType().getTypeName();
        Class aClass = null;
        try {
            aClass = Class.forName(typeName);
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                VerifyParam annotation = declaredField.getAnnotation(VerifyParam.class);
                if (annotation==null)
                    continue;
                declaredField.setAccessible(true);
                Object resultValue = declaredField.get(value);
                checkValue(value,annotation,parameterName);
            }
        } catch (ClassNotFoundException e) {
            throw new BusinessException(600,"服务器错误正则匹配不通过");
        }catch (BusinessException e){
            throw e;
        } catch (IllegalAccessException e) {
            throw new BusinessException(600,"服务器错误正则匹配不通过");
        }
    }
}
