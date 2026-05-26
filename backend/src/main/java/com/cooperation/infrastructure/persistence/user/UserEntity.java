package com.cooperation.infrastructure.persistence.user;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户表持久化实体，映射 {@code users} 表的基础字段。
 */
@TableName("users")
public class UserEntity {

    /**
     * 用户唯一标识，对应自增主键 {@code users.id}。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户展示名称，对应 {@code users.display_name}。
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 用户邮箱，对应 {@code users.email}。
     */
    @TableField("email")
    private String email;

    /**
     * 用户状态，对应 {@code users.status}。
     */
    @TableField("status")
    private String status;

    /**
     * 创建时间，对应 {@code users.created_at}。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间，对应 {@code users.updated_at}。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 获取用户唯一标识。
     *
     * @return 用户唯一标识
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置用户唯一标识。
     *
     * @param id 用户唯一标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户展示名称。
     *
     * @return 用户展示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 设置用户展示名称。
     *
     * @param displayName 用户展示名称
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取用户邮箱。
     *
     * @return 用户邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置用户邮箱。
     *
     * @param email 用户邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取用户状态。
     *
     * @return 用户状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置用户状态。
     *
     * @param status 用户状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 获取更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 设置更新时间。
     *
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
