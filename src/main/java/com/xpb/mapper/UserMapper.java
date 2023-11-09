package com.xpb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xpb.entities.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}
