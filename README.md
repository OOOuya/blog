# 博客重构笔记

# 1. 技术架构

后端：

mybatis + springboot + oAuth2

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
