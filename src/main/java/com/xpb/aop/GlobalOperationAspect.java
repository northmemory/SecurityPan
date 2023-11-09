package com.xpb.aop;


import com.xpb.aop.annotation.GlobalInterceptor;
import com.xpb.aop.annotation.VerifyParam;
import com.xpb.utils.RegexUtil;
import com.xpb.utils.exceptions.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class GlobalOperationAspect {
    @Pointcut("@annotation(interceptor)")
    private void requestInterceptor(GlobalInterceptor interceptor){
    }
    @Before("requestInterceptor(interceptor)")
    public void BeforeAdvice(JoinPoint pointcut, GlobalInterceptor interceptor) throws NoSuchMethodException {
        Object target = pointcut.getTarget();
        Object[] args = pointcut.getArgs();
        String methodName=pointcut.getSignature().getName();
        Class<?>[] parameterTypes = ((MethodSignature) pointcut.getSignature()).getMethod().getParameterTypes();
        Method method= target.getClass().getMethod(methodName,parameterTypes);
        if (interceptor.checkParams()){

        }
    }

    public void validateParams(Method m,Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter=parameters[i];
            String parameterName=parameter.getName();
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
        Boolean isEempty= value==null || value.toString()=="";
        Integer length=value==null ? 0 : value.toString().length();
        //校验空值
        if (isEempty && verifyParam.required())
            throw new BusinessException(600,parameterName+"参数为空");
        //检验长度
        if (!isEempty && (verifyParam.max()!=-1 && verifyParam.max()<length || verifyParam.min()!=-1 && verifyParam.min()>length))
            throw new BusinessException(600,parameterName+"参数过长");
        //校验正则
        if ((!isEempty && verifyParam.regexVerify().getRegex()!="") && !RegexUtil.verify(value.toString(),verifyParam.regexVerify().getRegex()))
            throw new BusinessException(600,parameterName+"正则匹配不通过");
    }

    public void checkObjValue(Parameter parameter, Object value, String parameterName){
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
            throw new RuntimeException(e);
        }catch (BusinessException e){
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
