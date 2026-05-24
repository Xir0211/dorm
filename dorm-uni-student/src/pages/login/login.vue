<template>
  <view class="login-page">
    <view class="bg-shape-large"></view>
    
    <view class="content-wrapper">
      <view class="login-header">
        <view class="header-circles">
          <view class="circle-blue"></view>
          <view class="circle-yellow"></view>
        </view>
        <text class="login-title">🏠 汤臣一品</text>
      </view>
  
      <view class="login-form">
        <view class="input-group">
          <input 
            class="input-field" 
            v-model="username" 
            placeholder="请输入学号" 
            placeholder-style="color: #c0c4cc"
          />
        </view>
  
        <view class="input-group">
          <input 
            class="input-field" 
            v-model="password" 
            type="password" 
            placeholder="请输入密码"
            placeholder-style="color: #c0c4cc"
          />
        </view>
  
        <view class="captcha-row-wrapper">
          <input 
            class="input-field captcha-input-left" 
            v-model="captcha" 
            placeholder="验证码" 
            maxlength="4"
            placeholder-style="color: #c0c4cc"
          />
          <view class="captcha-code-right" @click="getCaptcha">
            <text 
              v-for="(ch, i) in captchaChars" 
              :key="i" 
              class="captcha-char"
              :style="{ 
                color: ch.color, 
                fontSize: ch.size + 'px', 
                transform: 'rotate(' + ch.rotate + 'deg)',
                fontWeight: ch.weight,
                pointerEvents: 'none'
              }"
            >{{ ch.char }}</text>
          </view>
        </view>
  
        <button class="login-btn" @click="handleLogin" :loading="loading" :disabled="loading">
            {{ loading ? '登录中...' : '立即登录' }}
        </button>
  
        <!-- <view class="login-tips" @click="goToForget">
          <text class="tips-text">忘记密码？</text>
        </view> -->
      </view>
    </view>

    <view class="agreement-footer">
      <checkbox-group class="checkbox-wrapper" @change="handleAgreementChange">
        <checkbox value="agree" :checked="isAgree" color="#f8dc74" style="transform:scale(0.7)" />
        <text class="agreement-text" @click="toggleAgreement">我已阅读并同意《用户协议》和《隐私协议》</text>
      </checkbox-group>
    </view>
  </view>
</template>
  
<script setup>
import { ref, onMounted } from 'vue';
import BASE_URL from '../common/config';

const username = ref('');
const password = ref('');
const captcha = ref('');
const captchaChars = ref([]);
const captchaUuid = ref('');
const loading = ref(false);

// 💥 新增：是否同意协议的状态变量
const isAgree = ref(false);

const colors = ['#e74c3c', '#e67e22', '#2ecc71', '#3498db', '#9b59b6', '#1abc9c', '#f39c12', '#e91e63'];

// 监听复选框勾选状态变更
const handleAgreementChange = (e) => {
  isAgree.value = e.detail.value.includes('agree');
};

// 点击协议文字也能切换勾选状态（提升用户体验）
const toggleAgreement = () => {
  isAgree.value = !isAgree.value;
};

// 跳转到忘记密码
const goToForget = () => {
  uni.navigateTo({
    url: '/pages/login/forget'
  });
};

// 获取验证码
const getCaptcha = async () => {
  try {
    const res = await uni.request({ url: BASE_URL + '/auth/captcha', method: 'GET' });
    const result = res.data;
    if (result.code === 200) {
      captchaUuid.value = result.data.uuid;
      captcha.value = '';
      const codeText = result.data.code || 'ABCD';
      captchaChars.value = codeText.split('').map(ch => ({
        char: ch,
        rotate: Math.floor(Math.random() * 30) - 15,
        color: colors[Math.floor(Math.random() * colors.length)],
        size: 22 + Math.floor(Math.random() * 6),
        weight: Math.random() > 0.5 ? 'bold' : 'normal',
      }));
    }
  } catch (e) {
    uni.showToast({ title: '获取验证码失败', icon: 'none' });
  }
};

// 登录
const handleLogin = async () => {
  console.log('开始登录...');

  // 💥 核心拦截逻辑：优先校验协议是否勾选
  if (!isAgree.value) {
    uni.showToast({ title: '请阅读并勾选用户协议', icon: 'none' });
    return;
  }

  // 前端字段校验
  if (!username.value.trim()) {
    uni.showToast({ title: '请输入学号', icon: 'none' });
    return;
  }
  if (!password.value.trim()) {
    uni.showToast({ title: '请输入密码', icon: 'none' });
    return;
  }
  if (!captcha.value.trim()) {
    uni.showToast({ title: '请输入验证码', icon: 'none' });
    return;
  }
  if (!captchaUuid.value) {
    uni.showToast({ title: '验证码已失效，请刷新', icon: 'none' });
    return;
  }

  loading.value = true;
  try {
    const res = await uni.request({
      url: BASE_URL + '/auth/login',
      method: 'POST',
      header: { 'Content-Type': 'application/json' },
      data: {
        username: username.value.trim(),
        password: password.value.trim(),
        captchaCode: captcha.value.trim(),
        captchaKey: captchaUuid.value,
      },
    });

    const result = res.data;

    if (result.code === 200) {
      const user = result.data.user;

      // 校验角色必须是 STUDENT
      if (user.role !== 'STUDENT') {
        uni.showToast({ title: '该账号非学生，无法登录', icon: 'none' });
        getCaptcha();
        return;
      }

      // 登录成功存储
      uni.setStorageSync('token', result.data.token);
      uni.setStorageSync('userInfo', JSON.stringify({
        userId: user.userId,
        username: user.username,
        realName: user.realName,
        gender: user.gender,
        department: user.department,
        className: user.className,
        phone: user.phone,
        role: user.role,
      }));

      uni.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => {
          console.log("跳转到主页");
          uni.reLaunch({ url: '/pages/index/index' });
      }, 500);
    } else {
      uni.showToast({ title: result.msg || '登录失败', icon: 'none' });
      getCaptcha();
    }
  } catch (e) {
    uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
    console.error(e);
  } finally {
    loading.value = false;
  }
};

onMounted(getCaptcha);
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 1;
}

.bg-shape-large {
  position: absolute;
  top: -150rpx;
  right: -150rpx;
  width: 650rpx;
  height: 650rpx;
  background-color: #fdfaf0; 
  border-radius: 50%;
  z-index: 0; 
}

.content-wrapper {
  position: relative;
  z-index: 10; 
  width: 100%;
  padding: 160rpx 60rpx 0;
  box-sizing: border-box;
}

.header-circles {
  display: flex;
  margin-bottom: 24rpx;
}
.circle-blue {
  width: 70rpx;
  height: 70rpx;
  background-color: #d7f4f8;
  border-radius: 50%;
}
.circle-yellow {
  width: 70rpx;
  height: 70rpx;
  background-color: #fce47c;
  border-radius: 50%;
  margin-left: -20rpx; 
}

.login-header {
  margin-bottom: 80rpx;
}
.login-title {
  font-size: 52rpx;
  font-weight: bold;
  color: #000000;
  letter-spacing: 2rpx;
  display: block;
}

.login-form {
  width: 100%;
}

.input-group {
  margin-bottom: 40rpx;
}

.input-field {
  width: 100%;
  height: 96rpx;
  background: #ffffff;
  border: 2rpx solid #efefef;
  border-radius: 48rpx;
  padding: 0 40rpx;
  font-size: 30rpx;
  color: #333;
  box-sizing: border-box;
}

.captcha-row-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-bottom: 40rpx;
  width: 100%;
}

.captcha-input-left {
  flex: 1;
}

.captcha-code-right {
  width: 200rpx;
  height: 96rpx;
  background: #fdfaf0; 
  border: 2rpx solid #fce47c;
  border-radius: 48rpx; 
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  flex-shrink: 0;
  overflow: hidden;
  box-sizing: border-box;
  position: relative;
  z-index: 20;
}

.captcha-char {
  font-family: 'Georgia', 'Times New Roman', serif;
  display: inline-block;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(90deg, #f8dc74 0%, #d0f2e7 100%);
  color: #333333; 
  font-size: 32rpx;
  font-weight: bold;
  border-radius: 48rpx;
  border: none;
  margin-top: 20rpx;
  letter-spacing: 4rpx;
  position: relative;
  z-index: 20;
}
.login-btn::after {
  border: none;
}

.login-tips {
  text-align: center;
  margin-top: 32rpx;
}
.tips-text {
  font-size: 26rpx;
  color: #79d7cc; 
}

.agreement-footer {
  position: absolute;
  bottom: 60rpx;
  left: 0;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 20;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
}

.agreement-text {
  font-size: 24rpx;
  color: #999999;
  padding-left: 8rpx; /* 增加一点间距 */
}
</style>