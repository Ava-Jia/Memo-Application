package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 数据库连接管理器
 * 使用HikariCP连接池管理SQLite数据库连接
 */

public class DatabaseManager {
    // 创建日志记录器，用于记录重要的操作和错误信息
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    
    // 数据库配置常量
    private static final String DB_URL = "jdbc:sqlite:todos.db";
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 5;
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    
    // 连接池实例
    private static volatile HikariDataSource dataSource;
    
    // 私有构造函数防止实例化
    private DatabaseManager() {
        throw new IllegalStateException("工具类不应该被实例化");
    }
    
    /**
     * 初始化连接池，使用双重检查锁定确保线程安全
     * 这个方法会配置并创建HikariCP连接池
     */
    public static void initializePool() {
        // 如果连接池已经存在且没有关闭，直接返回
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        
        // 使用同步块确保线程安全
        synchronized (DatabaseManager.class) {
            if (dataSource != null && !dataSource.isClosed()) {
                return;
            }
            
            try {
                logger.info("开始初始化数据库连接池");
                
                HikariConfig config = createConfig();
                dataSource = new HikariDataSource(config);
                
                // 验证连接池是否正常工作
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(3)) {
                        logger.info("数据库连接池初始化成功");
                    }
                }
            } catch (SQLException | HikariPool.PoolInitializationException e) {
                logger.error("初始化数据库连接池失败", e);
                throw new DatabaseException("无法初始化数据库连接池", e);
            }
        }
    }
    
    /**
     * 创建HikariCP配置
     * 设置所有必要的连接池参数
     */
    private static HikariConfig createConfig() {
        HikariConfig config = new HikariConfig();
        
        // 基础配置
        config.setJdbcUrl(DB_URL);
        config.setPoolName("TodoPool");  // 便于监控和调试的连接池名称
        
        // 连接池大小配置
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        
        // 连接超时配置
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setIdleTimeout(IDLE_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);
        
        // SQLite特定配置
        config.addDataSourceProperty("foreign_keys", "true");  // 启用外键约束
        
        // 性能优化配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // 连接测试配置
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(5));
        
        return config;
    }   
    
    /**
     * 获取数据库连接
     * 如果连接池未初始化，会自动进行初始化
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initializePool();
        }
        
        try {
            Connection conn = dataSource.getConnection();
            if (!conn.isValid(3)) {
                logger.warn("获取到无效连接，尝试重新获取");
                conn.close();
                return dataSource.getConnection();
            }
            return conn;
        } catch (SQLException e) {
            logger.error("获取数据库连接失败", e);
            throw new DatabaseException("无法获取数据库连接", e);
        }
    }
    
    /**
     * 获取连接池状态信息
     * 用于监控和调试目的
     */
    public static String getPoolStats() {
        if (dataSource != null) {
            return String.format(
                "连接池状态 - 活动连接: %d, 空闲连接: %d, 等待线程: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        }
        return "连接池未初始化";
    }
    
    /**
     * 关闭连接池
     * 在应用程序关闭时调用此方法以释放资源
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                logger.info("开始关闭数据库连接池");
                dataSource.close();
                logger.info("数据库连接池已成功关闭");
            } catch (Exception e) {
                logger.error("关闭数据库连接池时发生错误", e);
            } finally {
                dataSource = null;
            }
        }
    }
}

/**
 * 自定义数据库异常类
 * 用于封装和统一处理数据库相关错误
 */
class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}