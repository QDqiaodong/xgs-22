import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    } else {
      // 保留业务码(如 409 并发冲突)与后端返回的最新数据, 供页面区分提示与刷新
      const error = new Error(res.message || '请求失败')
      error.code = res.code
      error.data = res.data
      return Promise.reject(error)
    }
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
