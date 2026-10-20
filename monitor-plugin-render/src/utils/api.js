import axios from 'axios'
import {router} from "@/router";

export const request = axios.create({
        withCredentials:true,    //设置跨域携带cookie
    });

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    let token = sessionStorage.getItem("token") ? sessionStorage.getItem("token") : null
    if (token) {
        config.headers['token'] = token;  // 设置请求头
    }

    return config
}, error => {
    return Promise.reject(error)
});

axios.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    let token = sessionStorage.getItem("token") ? sessionStorage.getItem("token") : null
    if (token) {
        config.headers['token'] = token;  // 设置请求头
    }

    return config
}, error => {
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return res
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        // 当权限验证不通过的时候给出提示
        if (res.code === 401) {
            // this.$message({
            //     message: res.msg,
            //     type: 'error'
            // });
            router.replace("/")
        }
        return response;
    },
    error => {
        console.log('err' + error) // for debug
        return Promise.reject(error)
    }
);
