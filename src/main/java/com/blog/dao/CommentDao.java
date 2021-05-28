package com.blog.dao;

import com.blog.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommentDao {

    //根据创建时间倒序来排
    //查询
    List<Comment> findByBlogIdAndParentCommentNull(@Param("blogId") Long blogId, @Param("commentParentId") Long commentParentId);

    //查询父级对象
    Comment findByParentCommentId(@Param("parentCommentId")Long parentcommentid);

    //添加一个评论
    int saveComment(Comment comment);

    //通过根评论得到对应的子评论列表
    List<Comment> getReplyCommentsByrootId(@Param("rootId")Long rootId);
}
