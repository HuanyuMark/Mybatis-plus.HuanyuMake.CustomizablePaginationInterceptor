package org.HuanyuMake.CustomizablePaginationInterceptor;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Date: 2023/2/13 21:53
 * 重写了发出 "select count(*)"sql 的 autoCountSql 的方法, 可自定义总记录查询sql
 * @author HuanyuMake
 * @version 0.0.2
 */

@Configuration
// 定义从mybatis-plus.configuration.customizable-pagination-interceptor 前缀的application文件中读取属性配置该Bean
@ConfigurationProperties("mybatis-plus.configuration.customizable-pagination-interceptor") 
public class CustomizablePaginationInterceptor extends PaginationInnerInterceptor {
    protected String countField = "COUNT(1)";
    protected String countFieldAlias = "total";
    protected String countSqlSuffix = "_COUNT";
    protected Boolean openMapperCountSql = false;
    protected MappedStatement lastMs;
    protected List<SelectItem> COUNT_SELECT_ITEM = Collections.singletonList(
            (
                    new SelectExpressionItem(
                            (new Column()).withColumnName(countField)
                    )
            ).withAlias(new Alias(countFieldAlias))
    );  // 相当于 COUNT(1) AS total

    public boolean willDoQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        /*更新此次使用的MappedStatement,方便在 autoCountSql中使用*/
        lastMs = ms;
        return super.willDoQuery(executor , ms, parameter,rowBounds,resultHandler,boundSql);
    }

    @Override
    protected String lowLevelCountSql(String originalSql) {
        // 此处也使用自定义的总数查询sql
        return String.format("SELECT %s AS %s FROM (%s) TOTAL", countField,countFieldAlias,originalSql);
    }
    @Override
    protected String autoCountSql(@NotNull IPage<?> page, String sql) {
        if(openMapperCountSql) {
            // 尝试在mapper中查找是否有 countSqlSuffix 后缀的SQL,
            MappedStatement ms = null;
            try {
                ms = lastMs.
                        getConfiguration()
                        .getMappedStatement(lastMs.getId() + countSqlSuffix);
            } catch (Exception ignore){}
            // 若找到对应的count statement
            if(ms != null) {
                // 若statement的返回值不是 java.lang.Long类型,则抛出 IllegalResultType 错误
                if (ms.getResultMaps().get(0).getType() != Long.class) {
                    throw new IllegalResultType(
                            String.format("Statement '%s%s' return an illegal result type '%s',\n it must be '%s' type",
                                    lastMs.getId(),countSqlSuffix, ms.getResultMaps().get(0).getType(), Long.class.getName()));
                }
                // 返回正确的statement对应的sql字符串, 形如 "SELECT COUNT(1) AS total FROM user"
                return ms.getBoundSql(null)
                        .getSql();
            }
        }

        /*
         * 以下和 PaginationInnerInterceptor 父类的片段大体相同, 没有进行其它的逻辑处理, 
         * 之所以要抄过来是因为要使用本类的COUNT_SELECT_ITEM来充当查询总记录数的字段
        */ 
        if (!page.optimizeCountSql()) {
            return this.lowLevelCountSql(sql);
        } else {
            try {
                Select select = (Select) CCJSqlParserUtil.parse(sql);
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof SetOperationList) {
                    return this.lowLevelCountSql(sql);
                }

                PlainSelect plainSelect = (PlainSelect) selectBody;
                Distinct distinct = plainSelect.getDistinct();
                GroupByElement groupBy = plainSelect.getGroupBy();
                List<OrderByElement> orderBy = plainSelect.getOrderByElements();
                if (CollectionUtils.isNotEmpty(orderBy)) {
                    boolean canClean = true;
                    if (groupBy != null) {
                        canClean = false;
                    }

                    if (canClean) {
                        Iterator var10 = orderBy.iterator();

                        while (var10.hasNext()) {
                            OrderByElement order = (OrderByElement) var10.next();
                            Expression expression = order.getExpression();
                            if (!(expression instanceof Column) && expression.toString().contains("?")) {
                                canClean = false;
                                break;
                            }
                        }
                    }

                    if (canClean) {
                        plainSelect.setOrderByElements((List) null);
                    }
                }

                Iterator var21 = plainSelect.getSelectItems().iterator();

                while (var21.hasNext()) {
                    SelectItem item = (SelectItem) var21.next();
                    if (item.toString().contains("?")) {
                        return this.lowLevelCountSql(select.toString());
                    }
                }

                if (distinct == null && null == groupBy) {
                    if (this.optimizeJoin && page.optimizeJoinOfCountSql()) {
                        List<Join> joins = plainSelect.getJoins();
                        if (CollectionUtils.isNotEmpty(joins)) {
                            boolean canRemoveJoin = true;
                            String whereS = (String) Optional.ofNullable(plainSelect.getWhere()).map(Object::toString).orElse("");
                            whereS = whereS.toLowerCase();
                            Iterator var25 = joins.iterator();

                            while (var25.hasNext()) {
                                Join join = (Join) var25.next();
                                if (!join.isLeft()) {
                                    canRemoveJoin = false;
                                    break;
                                }

                                FromItem rightItem = join.getRightItem();
                                String str = "";
                                if (rightItem instanceof Table) {
                                    Table table = (Table) rightItem;
                                    str = (String) Optional.ofNullable(table.getAlias()).map(Alias::getName).orElse(table.getName()) + ".";
                                } else if (rightItem instanceof SubSelect) {
                                    SubSelect subSelect = (SubSelect) rightItem;
                                    if (subSelect.toString().contains("?")) {
                                        canRemoveJoin = false;
                                        break;
                                    }

                                    str = subSelect.getAlias().getName() + ".";
                                }

                                str = str.toLowerCase();
                                if (whereS.contains(str)) {
                                    canRemoveJoin = false;
                                    break;
                                }

                                Iterator var27 = join.getOnExpressions().iterator();

                                while (var27.hasNext()) {
                                    Expression expression = (Expression) var27.next();
                                    if (expression.toString().contains("?")) {
                                        canRemoveJoin = false;
                                        break;
                                    }
                                }
                            }

                            if (canRemoveJoin) {
                                plainSelect.setJoins((List) null);
                            }
                        }
                    }
                    // 使用本类的 COUNT_SELECT_ITEM 来充当查询字段
                    plainSelect.setSelectItems(COUNT_SELECT_ITEM);
                    return select.toString();
                }

                return this.lowLevelCountSql(select.toString());
            } catch (JSQLParserException var18) {
                this.logger.warn("optimize this sql to a count sql has exception, sql:\"" + sql + "\", exception:\n" + var18.getCause());
            } catch (Exception var19) {
                this.logger.warn("optimize this sql to a count sql has error, sql:\"" + sql + "\", exception:\n" + var19);
            }
            return this.lowLevelCountSql(sql);
        }
    }

    public String getCountField() {
        return countField;
    }

    public CustomizablePaginationInterceptor setCountField(String countField) {
        this.countField = countField;
        this.COUNT_SELECT_ITEM = generateSelectCountFields();
        return this;
    }

    public String getCountFieldAlias() {
        return countFieldAlias;
    }

    public CustomizablePaginationInterceptor setCountFieldAlias(String countFieldAlias) {
        this.countFieldAlias = countFieldAlias;
        this.COUNT_SELECT_ITEM = generateSelectCountFields();
        return this;
    }

    public String getCountSqlSuffix() {
        return countSqlSuffix;
    }

    public CustomizablePaginationInterceptor setCountSqlSuffix(String countSqlSuffix) {
        this.countSqlSuffix = countSqlSuffix;
        return this;
    }

    public Boolean getOpenMapperCountSql() {
        return openMapperCountSql;
    }

    public CustomizablePaginationInterceptor setOpenMapperCountSql(Boolean openMapperCountSql) {
        this.openMapperCountSql = openMapperCountSql;
        return this;
    }

    protected List<SelectItem> generateSelectCountFields() {
        return Collections.singletonList(
                (
                        new SelectExpressionItem(
                                (new Column()).withColumnName(countField)
                        )
                ).withAlias(new Alias(countFieldAlias))
        );
    }

    public static class IllegalResultType extends RuntimeException {
        public IllegalResultType() {
        }

        public IllegalResultType(String message) {
            super(message);
        }

        public IllegalResultType(String message, Throwable cause) {
            super(message, cause);
        }

        public IllegalResultType(Throwable cause) {
            super(cause);
        }

        public IllegalResultType(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
