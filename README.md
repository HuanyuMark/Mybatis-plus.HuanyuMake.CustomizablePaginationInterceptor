# MybatisPlus.HuanyuMake.CustomizablePaginationInterceptor
Compare with the PaginationInnerInterceptor, the Interceptor supports user to custom the SQL to query total records of you other SQL result 
与Mybatis-plus官方提供的内置分页插件 PaginnationInnerInterceptor 相比，该拦截器支持用户自定义用来查总记录数的sql语句

## 在 application.yaml 中的设置
```yaml
mybatis-plus:
  type-aliases-package: org.HuanyuMake..pojo 
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
    cache-enabled: true
    # 以下设置 CustomizablePaginationInterceptor Bean的属性
    customizable-pagination-interceptor:
      count-field: 'COUNT(1)'   # 自定义 select countField from (xx) 的countField默认内容
      count-field-alias: 'total' # 自定义 select countField AS countFieldAlias from (xx) countFieldAlias 内容
      open-mapper-count-sql: true  # 开启对同mapper中 总记录查询语句的<select>字句的使用, 默认为 false
      count-sql-suffix: '_COUNT' # 选择使用什么后缀的同id<select>子句作为总记录数查询sql, 默认值'_COUNT'
      max-limit: 2000  # 自定义一页最多有多少条记录数
      
 ```
 ## 其它说明
 ### 1. 如果不设置 ```open-mapper-count-sql```为 ```true```, 则不能自动使用自定义sql来查询总记录数
 使用以上配置, 例子:
 ```xml
     <select id="selectAllSubscriberById" parameterType="int" resultType="user">
        SELECT subscriber.*
        FROM users AS blogger, subscriptions, users AS subscriber
        WHERE blogger.id = #{id} AND subscriptions.blogger_user_id = blogger.id
          AND subscriber.id = subscriptions.subscriber_user_id
    </select>

    <!-- 设置了open-mapper-count-sql = true , 且 count-sql-suffix = '_COUNT' 则会使用该句查询总记录数 -->
    <!-- 这是自定义 selectAllSubscriberById sql的总数查询sql的sql语句, 但要注意这些语句的 resultType 必须为 Long-->
    <select id="selectAllSubscriberById_COUNT" parameterType="int" resultType="Long">
        SELECT subscriber.*
        FROM users AS blogger, subscriptions, users AS subscriber
        WHERE blogger.id = #{id} AND subscriptions.blogger_user_id = blogger.id
          AND subscriber.id = subscriptions.subscriber_user_id
    </select>
 ```
