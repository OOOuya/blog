# 博客重构笔记

# 1. 技术架构

后端：

mybatis + springboot

### 引入mybatis

```yml
#    配置mybatis
#设置别名
mybatis:
  type-aliases-package: com.anrolsp.POJO
  #指定mapper文件
  mapper-locations: classpath:mapper/*Mapper.xml
```

# 2.项目架构

![ViewImage](https://i.loli.net/2021/05/23/SK9ZeAUFgpiMzI2.png)


## 2.1 POJO

### 2.1.1 blog

```java
private Long id;
/*content需要更改为大字段类型，否则字数过多溢出会报异常*/
private String content;
private String firstPicture;
private String title;
private String flag;
private Integer views;
private boolean appreciation;
private boolean copyright;
private boolean commentable;
private boolean published;
private boolean recommend;
private String description;
/*时间的注解*/
private Date createTime;
/*时间的注解*/
private Date updateTime;
/*blog和type多对一，因此blog中有一个type对象*/
private Type type;
//blog和tag是多对多
private List<Tag> tags = new ArrayList<>();
private User user;
private List<Comment> comments = new ArrayList<>();
/*得到多个tagid，对应blogs-inputs页面的tagIds变量，不计入数据库*/
private String tagIds;
```

### 2.1.2 comment

评论有评论时间，评论昵称，主键id，邮箱，内容，头像，博客对象（博客和评论一对多）

**评论还需要有父、子评论两个对象。**还有博主的身份标志。

```java
private Long id;
private String nickname;
private String email;
private String content;
private String avatar;
/*时间的注解*/
private Date createTime;
private Blog blog;
/*定义评论间的关联关系
* 一个父类可以有多个子类对象，一个子类对象只能有（相连的）一个父类对象
* */
private List<Comment> replyComments = new ArrayList<Comment>();
private Comment parentComment;
/*判断是不是博主*/
private boolean adminComment;
```

### 2.1.3 Type分类

只需要包含分类id和名称

```java
private Long id;
private String name;
```

### 2.1.4 tag分类

```java
private Long id;
private String name;
private List<Blog> blogs = new ArrayList<>();
```

### 2.1.5 user

```java
private Long id;
private String nickname;
private String password;
private String username;
private String email;
private String avatar;
private Integer type;
private Date createTime;
private Date updateTime;
private List<Blog> blogs = new ArrayList<>();
```







## 2.2 功能模块

### 2.2.1 后台

#### 博客

##### 定义博客的resultmap

```xml
<resultMap id="blog" type="Blog">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="content" column="content"/>
    <result property="flag" column="flag"/>
    <result property="views" column="views"/>
    <result property="updateTime" column="update_time"/>
    <result property="typeId" column="type_id"/>
    <result property="firstPicture" column="first_picture"/>
    <result property="shareStatement" column="share_statement"/>
    <result property="published" column="published"/>
    <result property="appreciation" column="appreciation"/>
    <result property="commentabled" column="commentabled"/>
    <result property="description" column="description"/>
    <result property="recommend" column="recommend"/>
    <result property="createTime" column="create_time"/>
    <result property="typeId" column="type_id"/>
    <result property="userId" column="user_id"/>
    <result property="tagIds" column="tag_ids"/>
    <association property="type" javaType="Type">
        <id property="id" column="typeid"/>
        <result property="name" column="typename"/>
    </association>
    <association property="user" javaType="User">
        <id property="id" column="uid"/>
        <result property="nickname" column="nickname"/>
        <result property="username" column="username"/>
        <result property="email" column="email"/>
        <result property="avatar" column="avatar"/>
    </association>
    <collection property="tags" ofType="Tag">
        <id property="id" column="tagid"/>
        <result property="name" column="tagname"/>
    </collection>
</resultMap>
```



##### 后台展示博客详情

```java
@Override
public Blog getBlog(Long id) {
    return blogDao.getBlog(id);
}
```

```xml
<select id="getBlog" resultMap="blog"> /*后台展示博客*/
    select b.id, b.published, b.flag, b.title, b.content, b.type_id,
     b.tag_ids, b.first_picture, b.description, b.recommend,
     b.share_statement, b.appreciation, b.commentabled
    from t_blog b  where  b.id = #{id};
</select>
```

##### 后台得到所有的博客

不展示标签

```java
@Override
public List<Blog> getAllBlog() {
    return blogDao.getAllBlog();
}
```

```
<select id="getAllBlog" resultMap="blog">  /*后台博客展示*/
    select b.id, b.title, b.update_time, b.recommend, b.type_id, b.published,
           t.id typeid, t.name typename
    from t_blog b, t_type t
    where b.type_id = t.id    /*博客类型id=类型id*/
</select>
```

##### 后台搜索博客

```java
@Override
public List<Blog> searchAllBlog(Blog blog) {
    return blogDao.searchAllBlog(blog);
}
```

```xml
<select id="searchAllBlog" parameterType="Blog" resultMap="blog">
    <bind name="pattern" value="'%' + title + '%'" />    /*模糊查询*/
    select b.id, b.title, b.update_time, b.recommend, b.published, b.type_id, t.id, t.name
    from t_blog b ,t_type t
    <where>
        <if test="1 == 1">
            b.type_id = t.id    /*博客类型id=类型id*/
        </if>
        <if test="typeId != null">
            and b.type_id = #{typeId}       /*根据博客类型查询*/
        </if>
        <if test="recommend != null">
            and b.recommend = #{recommend}   /*根据博客推荐查询*/
        </if>
        <if test="title != null">
            and b.title like #{pattern}   /*根据博客title模糊查询*/
        </if>
    </where>
</select>
```

##### 新增博客

```java
@Override    //新增博客
public int saveBlog(Blog blog) {
    blog.setCreateTime(new Date());
    blog.setUpdateTime(new Date());
    blog.setViews(0);
    //保存博客
    blogDao.saveBlog(blog);
    //保存博客后才能获取自增的id
    Long id = blog.getId();
    //将标签的数据存到t_blogs_tag表中
    List<Tag> tags = blog.getTags();
    BlogAndTag blogAndTag = null;
    for (Tag tag : tags) {
        //新增时无法获取自增的id,在mybatis里修改
        blogAndTag = new BlogAndTag(tag.getId(), id);
        blogDao.saveBlogAndTag(blogAndTag);
    }
    return 1;
}
```

```xml
  <!--useGeneratedKeys="true"；使用自增主键获取主键值策略
    keyProperty；指定对应的主键属性，也就是mybatis获取到主键值以后，将这个值封装给javaBean的哪个属性
-->
   <insert id="saveBlog" parameterType="Blog" useGeneratedKeys="true" keyProperty="id">
       insert into t_blog (title, content, first_picture, flag,
       views, appreciation, share_statement, commentabled,published,
       recommend, create_time, update_time, type_id, tag_ids, user_id, description)
       values (#{title}, #{content}, #{firstPicture}, #{flag}, #{views}, #{appreciation},
       #{shareStatement}, #{commentabled}, #{published}, #{recommend}, #{createTime},
       #{updateTime}, #{typeId}, #{tagIds}, #{userId}, #{description});
   </insert>

   <insert id="saveBlogAndTag" parameterType="BlogAndTag">
       insert into t_blog_tags (tag_id, blog_id) values (#{tagId},#{blogId});
   </insert>
```



#### 评论

##### 评论的resultmap



##### 展示二级评论

通过博客

```xml
   <resultMap id="comment" type="Comment">
        <id property="id" column="cid"/>
        <result property="nickname" column="nickname"/>
        <result property="email" column="email"/>
        <result property="content" column="content"/>
        <result property="adminComment" column="admin_comment"/>
        <result property="avatar" column="avatar"/>
        <result property="createTime" column="create_time"/>
        <result property="blogId" column="blog_id"/>
        <result property="parentCommentId" column="parent_comment_id"/>
        <association property="blog" javaType="Blog">
            <id property="id" column="id"/>
        </association>
        <association property="parentComment" javaType="Comment">
            <id property="id" column="pid"/>
            <result property="nickname" column="pNickname"/>
        </association>
    </resultMap>
```

id得到父评论,找到`parent_comment_id`是根评论id的子评论，封装成一层list

```java

 //通过博客id得到父评论,找到parent_comment_id是根评论id的子评论，封装成一层
    public List<Comment> getCommentByBlogId(Long blogId) {  //查询父评论
        //没有父节点的默认为-1,得到所有的根评论
        List<Comment> comments = commentDao.findByBlogIdAndParentCommentNull(blogId, Long.parseLong("-1"));
        List<Comment> rootCommentList = getRootCommentList(comments);
        return getRootCommentList(rootCommentList);
    }

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

```

xml

```xml
<select id="findByBlogIdAndParentCommentNull" resultMap="comment">
    select c.id cid,c.nickname,c.email,c.content,c.avatar,
    c.create_time,c.blog_id,c.parent_comment_id, c.admin_comment
    from t_comment c, t_blog b
    where c.blog_id = b.id and c.blog_id = #{blogId} and c.parent_comment_id = #{commentParentId}
    order by c.create_time desc
</select>

 <select id="getReplyCommentsByrootId" resultMap="comment" parameterType="Long">
        select c.id cid,c.nickname,c.email,c.content,c.avatar,c.admin_comment,
        c.create_time,c.blog_id,c.parent_comment_id, c2.nickname pNickname, c2.id pid
		from t_comment c , t_comment c2
		where c.parent_comment_id = #{rootId} and c2.id = c.parent_comment_id
    </select>
```





##### 发布评论

```java
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
```



```xml
<select id="findByParentCommentId" resultMap="comment" parameterType="Long">
    select c.id cid, c.nickname, c.email, c.content, c.avatar,
    c.create_time, c.blog_id, c.parent_comment_id
    from t_comment c
    where c.id = #{parentCommentId}
</select>
```

### 2.2.2 前台模块

#### 博客

##### 详情页展示博客

将Markdown格式转换成html

如果博客没有标签，就不适用中间表使用

```java
 public Blog getDetailedBlog(Long id) {
        Blog blog;
        if (blogDao.getBlogAndTag(id).size() == 0){
            blog = blogDao.getDetailedBlogWithoutTag(id);
        }
        else
            blog = blogDao.getDetailedBlogAndTags(id);
        if (blog == null) {
            throw new NotFoundException("该博客不存在");
        }
        System.out.println(blog);
        String content = blog.getContent();
        blog.setContent(MarkdownUtils.markdownToHtmlExtensions(content));  //将Markdown格式转换成html
        return blog;
    }
```

```xml
 <!--前端展示博客（通过中间表得到标签）-->
    <select id="getDetailedBlogAndTags" resultMap="blog">
        select b.id, b.first_picture, b.flag, b.title, b.content, b.views,
        b.update_time,b.commentabled, b.share_statement, b.appreciation,
        u.nickname, u.avatar,
        tag.id tagid, tag.name tagname
        from t_blog b, t_user u, t_tag tag, t_blog_tags tb
        where b.user_id = u.id and tb.blog_id = b.id and tb.tag_id = tag.id and  b.id = #{id}
    </select>

    <!--得到不含标签的博客-->
    <select id="getDetailedBlogWithoutTag" resultMap="blog">
        select b.id, b.first_picture, b.flag, b.title, b.content, b.views,b.update_time,b.commentabled, b.share_statement, b.appreciation, b.description,
        u.nickname, u.avatar, u.id uid, u.email, u.avatar,
		t.id typeid, t.name typename
        from t_blog b join t_user u on b.user_id = u.id inner join t_type t on b.type_id = t.id
				where b.id = #{id}

    </select>
```

##### 通过分类id得到博客

通过更新时间排序

```java
@Override
public List<Blog> getByTypeId(Long typeId) {
    return blogDao.getByTypeId(typeId);
}
```

```xml
<select id="getByTypeId" resultMap="blog">
    select b.id, b.title, b.first_picture, b.views, b.update_time, b.description,
    t.name typename, t.id typeid,
    u.nickname, u.avatar
    from t_blog b, t_type t, t_user u
    where b.type_id = t.id and u.id = b.user_id and b.type_id = #{typeId} order by b.update_time desc
</select>
```

##### 通过标签id得到博客

```java
@Override
public List<Blog> getByTagId(Long tagId) {
    return blogDao.getByTagId(tagId);
}
```



##### 首页左侧展示博客

```java
@Override
public List<Blog> getIndexBlog() {
    return blogDao.getIndexBlog();
}
```

```xml
<select id="getIndexBlog" resultMap="blog">  /*主页博客展示*/
    select b.id, b.title, b.first_picture, b.views, b.update_time, b.description,
    t.name typename, t.id typeid,
    u.nickname, u.avatar
    from t_blog b, t_type t, t_user u
    where b.type_id = t.id and  u.id = b.user_id order by b.update_time desc
</select>
```

##### 首页右侧推荐博客

```java
@Override
public List<Blog> getAllRecommendBlog() {
    return blogDao.getAllRecommendBlog();
}
```

```xml
<select id="getAllRecommendBlog" resultMap="blog">
    select id, title, recommend from t_blog order by update_time desc;
</select>
```

##### 首页模糊查询博客

```java
@Override
public List<Blog> getSearchBlog(String query) {
    return blogDao.getSearchBlog(query);
}
```

```xml
<select id="getSearchBlog" resultMap="blog">
    <bind name="pattern" value="'%' + query + '%'" />
    select b.id, b.title, b.first_picture, b.views, b.update_time, b.description,
    t.name typename,
    u.nickname, u.avatar
    from t_blog b, t_type t, t_user u
    where b.type_id = t.id and  u.id = b.user_id and (b.title like #{pattern} or b.content like  #{pattern})
    order by b.update_time desc
</select>
```

##### 归档

根据博客的更新时间归档返回时间，通过时间（key）`List<Blog>`作为值插入Map

```java
@Override
public Map<String, List<Blog>> archiveBlog() {
    List<String> years = blogDao.findGroupYear();
    Set<String> set = new HashSet<>(years);  //set去掉重复的年份
    Map<String, List<Blog>> map = new HashMap<>();
    for (String year : set) {
        map.put(year, blogDao.findByYear(year));
    }
    return map;
}
```

```xml
<!--返回博客的时间列表-->
<select id="findGroupYear" resultType="String">
    select DATE_FORMAT(b.update_time, '%Y') from t_blog b order by b.update_time desc
</select>

<!--通过博客时间来查询年份对应的博客-->
<select id="findByYear" resultMap="blog">
    select b.title, b.update_time, b.id, b.flag
    from t_blog b
    where DATE_FORMAT(b.update_time, "%Y") = #{year}
</select>
```



## 3. 拦截器设置

设置拦截的配置类

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {   //配置拦截器
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin")
                .excludePathPatterns("/admin/login");
    }
}
```

设置拦截器规则（session，没取到就拦截）

```java
//登录拦截器
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getSession().getAttribute("user") == null){
            response.sendRedirect("/admin");
            return false;
        }
        return true;
    }
}
```



## 4. 前端页面优化

1. 更换博客详情页面中的markdown风格（更改成vue的markdown风格？）
2. 增加一些页面动态效果

## 5. 功能需完善

1. ouAth配置github登录
2. 加入Redis和Nginx反向代理

## 6.问题

### 图片加载出错

`GET http://127.0.0.1:8080/image/bg-002.gif 404`

### 解决生成目录不存在的问题

在blog.html定义了相关的js插件

```js
    /*自动生成目录插件*/
    tocbot.init({
       tocSelector: '.js-toc',
        /*内容元素*/
        contentSelector:'.js-toc-content',
        headingSelector:'h1 h2,h3'
    });
```

对应html的生成位置

```html
<div class="ui context">生成的目录</div>
    <!--tocbot生成的目录-->
    <ol class="js-toc"></ol>
```

静态网页有目录。

通过blog的content的markdown语法后台转换成html，其中`<h>`要定义生成id，这样才能生成目录

**定义的方法**

引入两个包，用于定义markdown转换的heading和table定义

```xml
<dependency>
    <groupId>com.atlassian.commonmark</groupId>
    <artifactId>commonmark-ext-heading-anchor</artifactId>
    <version>0.10.0</version>
</dependency>


<dependency>
    <groupId>com.atlassian.commonmark</groupId>
    <artifactId>commonmark-ext-gfm-tables</artifactId>
    <version>0.10.0</version>
</dependency>
```

### 解决页面加载过慢的问题

图片的资源加载占用了大部分时间。发现别人的网站大部分都是在内存缓存中的

![image-20210519225442326](https://i.loli.net/2021/05/19/By1Id6JHP49t7Y8.png)

而我的大部分是这样的![image-20210519225504966](https://i.loli.net/2021/05/19/8b4gpWNB9e3ODUR.png)

### 解决代码高亮不显示的问题

发现在网页上是代码高亮的？

![image-20210519232505001](https://i.loli.net/2021/05/19/mNlEOcJdvXx5twp.png)



### 数据库的逻辑问题

建立了博客和标签表，如果存入博客的时候没有设置标签

取出博客的时候又要使用到中间表，这样取不出来博客。

**解决方法**

> ### 解决方法
>
> 取博客的时候首先判断中间表中有没有blog_id,如果没有就不借助中间表获取标签。
>
> 如果有标签删除了，对应的中间表级联删除。



```xml

<!--得到不含标签的博客-->
<select id="getDetailedBlogWithoutTag" resultType="blog">
    select b.id, b.first_picture, b.flag, b.title, b.content, b.views,b.update_time,b.commentabled, b.share_statement, b.appreciation,
    u.nickname, u.avatar, u.id uid, u.email, u.avatar,
 t.id typeid, t.name typename
    from t_blog b, t_user u, t_type t
    where b.user_id = u.id  and b.id = #{id}
</select>
```

此时，对象一直为null
![image-20210526234256658](C:\Users\anrol\AppData\Roaming\Typora\typora-user-images\image-20210526234256658.png)

结果发现，resultType写错了，应该是resultMap

**在hibernate中**

> 新增博客，需要先加载所有的type和tag
>
> 原博客中，新增blog对象时，设置了tagids，从而设置了blog中的tag列表。
>
> 由于，blog对象的tags列表中设置了@ManyToMany属性，会在数据库创建一个`t_blog_tags`
>
> 最终调用了save方法，把`tag_id`和`blog_id`存入了`t_blog_tags`。取blog对象的时候，通过`blog_id`取出对象，并且从中间表里取出了`tag_id`



### 外键约束异常

`Cannot add or update a child row: a foreign key constraint fails (blog.t_comment, CONSTRAINT FK4jj284r3pb7japogvo6h72q95 FOREIGN KEY (parent_comment_id) REFERENCES t_comment (id))]`

t_comment中需要新建一个id=-1的一行，作为根目录，否则会出外键约束异常



### ajax返回值异常

调用load进行ajax请求，返回Model参数，但客户端似乎没有接收到服务端的参数。

```html
<script th:inline="javascript">
    var comments = [[${comments}]];
    console.log(comments);
</script>
```

添加上述语句，发现能显示一部分了？

![image-20210528093100724](C:\Users\anrol\AppData\Roaming\Typora\typora-user-images\image-20210528093100724.png)

去掉script语句又不行了，什么鬼？

发现传进来的comments没有设置parentComment属性。



### 主键重复

```xml
Duplicate entry '1' for key 'PRIMARY'
; Duplicate entry '1' for key 'PRIMARY'; nested exception is java.sql.SQLIntegrityConstraintViolationException: Duplicate entry '1' for key 'PRIMARY']
```

在数据库中设置主键为自增。



### 编辑博客找不到标签

报错空指针异常

```java
for (Tag tag : tags) {
            blogAndTag = new BlogAndTag(tag.getId(), blog.getId());
            blogDao.saveBlogAndTag(blogAndTag);
        }
```

似乎是因为把tagids存入blog表了，删除标签，但是blog中的tagids没有删除。

下次取得时候还是按照原来的标签来取，这样就空指针。

把tagids从数据库中删去



### 后台展示博客列表：无法展示分类

```xml
<if test="typeId != null">
    select b.id, b.title, b.update_time, b.recommend, b.type_id, b.published,
           t.id typeid, t.name typename
    from t_blog b, t_type t
    where b.type_id = t.id    /*博客类型id=类型id*/
</if>
```

似乎是这条语句中typeId没有判断出来

在serviceimpl进行判断，先取出所有的博客，如果有博客的分类id不是null，就设置对应的type对象，这样前端就能拿到分类对象了。


