package com.jt.plugins.api;

import org.pf4j.ExtensionPoint;

/**
 * @author blwy_qb
 */
public interface OperationButtonExtension extends ExtensionPoint {

    /**
     * 按钮名称
     *
     * @return 按钮名称
     */
    String name();

    /**
     * 响应点击事件
     *
     * @return 结果
     */
    String onClick();


}
