import instance from './axios';

export async function checkLogin(email, password) {
  const res = await instance.post(
    '/auth/login',   // 注意：没有 localhost
    //后端是RequestParam
    null, // body为空
    {
      params: {
        username: email,
        pass: password
      }
    }
  );
  // 登录成功时保存 token
  if (res.data.result === '登录成功' && res.data.token) {
    localStorage.setItem('authToken', res.data.token);
  }
  return res;
}

export async function register(email, password, username) {
  const res = await instance.post(
    '/auth/register',
    //后端是RequestParam
    null,
    {
      params: {
        username: username,
        pass: password,
        email: email
      }
    }
  );

  return res;
}

