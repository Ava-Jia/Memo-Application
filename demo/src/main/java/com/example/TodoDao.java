package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TodoDao {
    // 添加日志记录器，用于更好的错误跟踪和调试
    private static final Logger logger = LoggerFactory.getLogger(TodoDao.class);
    
    // 数据库配置常量
    private static final String DB_URL = "jdbc:sqlite:todos.db";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // SQL语句常量
    private static final String CREATE_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS todos (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
        "content TEXT NOT NULL," +
        "dateTime TEXT NOT NULL," +
        "priority TEXT NOT NULL" +
        ");";
    private static final String INSERT_TODO_SQL = 
        "INSERT INTO todos (content, dateTime, priority) VALUES (?, ?, ?)";
    private static final String SELECT_ALL_SQL = 
        "SELECT id, content, dateTime, priority FROM todos ORDER BY dateTime ASC";
    private static final String UPDATE_TODO_SQL = 
        "UPDATE todos SET content = ?, dateTime = ?, priority = ? WHERE id = ?";
    private static final String DELETE_TODO_SQL = 
        "DELETE FROM todos WHERE id = ?";

    // 数据源对象
    private final HikariDataSource dataSource;

    public TodoDao() {
        // 初始化数据源
        this.dataSource = initializeDataSource();
        createTable();
    }

    // 初始化数据源的私有方法
    private HikariDataSource initializeDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        
        return new HikariDataSource(config);
    }

    // 获取数据库连接的私有方法
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            logger.info("Todo表创建成功或已存在");
        } catch (SQLException e) {
            logger.error("创建Todo表失败", e);
            throw new DatabaseException("无法创建Todo表", e);
        }
    }

    public void addTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo对象不能为null");
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_TODO_SQL)) {
            pstmt.setString(1, todo.getContent());
            pstmt.setString(2, todo.getDateTime().format(FORMATTER));
            pstmt.setString(3, todo.getPriority().toString());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("成功添加Todo: {}", todo.getContent());
            } else {
                logger.warn("添加Todo失败: {}", todo.getContent());
            }
        } catch (SQLException e) {
            logger.error("添加Todo时发生错误", e);
            throw new DatabaseException("添加Todo失败", e);
        }
    }

    public List<Todo> getAllTodos() {
        List<Todo> todos = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                todos.add(buildTodoFromResultSet(rs));
            }
            logger.info("成功获取{}条Todo记录", todos.size());
            
        } catch (SQLException e) {
            logger.error("获取Todo列表时发生错误", e);
            throw new DatabaseException("获取Todo列表失败", e);
        }
        
        return todos;
    }

    // 从ResultSet构建Todo对象的辅助方法
    private Todo buildTodoFromResultSet(ResultSet rs) throws SQLException {
        return new Todo(
            rs.getInt("id"),
            rs.getString("content"),
            LocalDateTime.parse(rs.getString("dateTime"), FORMATTER),
            Priority.valueOf(rs.getString("priority"))
        );
    }

    public void updateTodo(Todo todo) {
        if (todo == null) {
            throw new IllegalArgumentException("Todo对象不能为null");
        }

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_TODO_SQL)) {
            pstmt.setString(1, todo.getContent());
            pstmt.setString(2, todo.getDateTime().format(FORMATTER));
            pstmt.setString(3, todo.getPriority().toString());
            pstmt.setInt(4, todo.getId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("成功更新Todo: ID={}", todo.getId());
            } else {
                logger.warn("更新Todo失败，可能不存在: ID={}", todo.getId());
            }
        } catch (SQLException e) {
            logger.error("更新Todo时发生错误: ID={}", todo.getId(), e);
            throw new DatabaseException("更新Todo失败", e);
        }
    }

    public void deleteTodo(int id) {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_TODO_SQL)) {
            pstmt.setInt(1, id);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("成功删除Todo: ID={}", id);
            } else {
                logger.warn("删除Todo失败，可能不存在: ID={}", id);
            }
        } catch (SQLException e) {
            logger.error("删除Todo时发生错误: ID={}", id, e);
            throw new DatabaseException("删除Todo失败", e);
        }
    }

    // 资源清理方法
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("数据源已关闭");
        }
    }
}

// 自定义数据库异常类
class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}