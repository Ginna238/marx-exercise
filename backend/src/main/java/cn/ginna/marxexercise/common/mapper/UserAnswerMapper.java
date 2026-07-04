package cn.ginna.marxexercise.common.mapper;

import cn.ginna.marxexercise.common.entity.UserAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    @Select("SELECT MAX(round_num) FROM user_answer WHERE user_id = #{userId} AND bank_id = #{bankId} AND question_type = #{questionType}")
    Integer getMaxRoundNum(@Param("userId") Long userId, @Param("bankId") Long bankId, @Param("questionType") String questionType);
}
