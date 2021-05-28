package com.blog.service.impl;

import com.blog.dao.BlogDao;
import com.blog.dao.CommentDao;
import com.blog.pojo.Comment;
import com.blog.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Sort;
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentDao commentDao;

    @Autowired
    private BlogDao blogDao;

    @Override
    //通过博客id得到父评论,找到parent_comment_id是根评论id的子评论，封装成一层
    public List<Comment> getCommentByBlogId(Long blogId) {  //查询父评论
        //没有父节点的默认为-1,得到所有的根评论
        List<Comment> comments = commentDao.findByBlogIdAndParentCommentNull(blogId, Long.parseLong("-1"));
        List<Comment> rootCommentList = getRootCommentList(comments);
        return getRootCommentList(rootCommentList);
    }

    List<Comment> oneLevelList = new ArrayList<Comment>();
    /**
     * description: 整合二级评论
     *
     * @Param: 父节点列表
     * @return 返回新的列表
     */
    @Override
    public List<Comment> getRootCommentList(List<Comment> comments) {
        List<Comment> replyComments = new ArrayList<Comment>();
        for (Comment comment : comments) {
            oneLevelList = new ArrayList<Comment>();
            Long rootId = comment.getId();
            List<Comment> replys = getReplyCommentsById(rootId);
            for (Comment reply : replys) {
                dfs(reply);
            }
            comment.setReplyComments(oneLevelList);
        }
        return comments;
    }

    //将一个评论的所有子评论都放到一层评论上
    private void dfs(Comment reply){
        oneLevelList.add(reply);
        List<Comment> replyComments = getReplyCommentsById(reply.getId());
        if(replyComments == null) return;
        for (Comment replyComment : replyComments) {
            dfs(replyComment);
        }
    }

    //通过根评论得到对应的子评论列表。
    private List<Comment> getReplyCommentsById(Long rootId){
        return commentDao.getReplyCommentsByrootId(rootId);
    }


    @Override
    //接收回复的表单
    public int saveComment(Comment comment) {
        //获得父id
        Long parentCommentId = comment.getParentComment().getId();
        comment.setParentCommentId(parentCommentId);
        //没有父级评论默认是-1
        if (parentCommentId != -1) {
            //有父级评论
            Comment ParentComment = commentDao.findByParentCommentId(parentCommentId);
            System.out.println(ParentComment);
            comment.setParentComment(ParentComment);
        } else {
            //没有父级评论
            comment.setParentCommentId((long) -1);
            comment.setParentComment(null);
        }
        comment.setCreateTime(new Date());
        return commentDao.saveComment(comment);
    }



}
