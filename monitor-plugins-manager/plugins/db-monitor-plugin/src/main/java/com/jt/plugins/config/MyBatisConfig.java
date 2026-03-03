package com.jt.plugins.config;

import com.jt.plugins.common.log.PluginLogger;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.InputStream;
/**
 * MyBatis配置管理器
 */
public class MyBatisConfig {

    private static final PluginLogger logger = PluginLogger.getLogger("db-monitor-plugin");
    private static SqlSessionFactory sqlSessionFactory;

    /**
     * 初始化 MyBatis 配置
     */
    public static void initialize(DataSource dataSource) {
        try {
            TransactionFactory transactionFactory = new JdbcTransactionFactory();
            Environment environment = new Environment("development", transactionFactory, dataSource);

            Configuration configuration = new Configuration(environment);
            configuration.setLazyLoadingEnabled(true);
            configuration.setAggressiveLazyLoading(false);

            // 注册 Mapper 接口
            configuration.addMapper(com.jt.plugins.mapper.SqlServerMetricsMapper.class);

            // 添加 XML 映射文件 - 正确的加载方式
            String xmlPath = "mapper/SqlServerMetricsMapper.xml";
            InputStream xmlStream = MyBatisConfig.class.getClassLoader().getResourceAsStream(xmlPath);
            
            if (xmlStream != null) {
                logger.info("找到 XML 映射文件：{}", xmlPath);
                
                // 使用 XMLMapperBuilder 解析 XML
                org.apache.ibatis.builder.xml.XMLMapperBuilder xmlMapperBuilder = 
                    new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                        xmlStream,
                        configuration,
                        xmlPath,
                        configuration.getSqlFragments()
                    );
                
                xmlMapperBuilder.parse();
                logger.info("XML 映射文件加载成功");
            } else {
                logger.warn("未找到 XML 映射文件：{}, 将使用注解方式", xmlPath);
            }

            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

            logger.info("MyBatis 配置初始化完成");

        } catch (Exception e) {
            logger.error("MyBatis 配置初始化失败", e);
            throw new RuntimeException("MyBatis 配置初始化失败", e);
        }
    }

    /**
     * 获取SqlSession
     */
    public static SqlSession getSqlSession() {
        if (sqlSessionFactory == null) {
            throw new IllegalStateException("MyBatis未初始化，请先调用initialize方法");
        }
        return sqlSessionFactory.openSession();
    }

    /**
     * 获取Mapper实例
     */
    public static <T> T getMapper(Class<T> mapperClass) {
        try (SqlSession sqlSession = getSqlSession()) {
            return sqlSession.getMapper(mapperClass);
        }
    }
}
