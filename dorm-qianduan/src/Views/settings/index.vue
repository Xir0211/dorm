<template>
    <div class="settings-page">
      <div class="page-header">
        <h2 class="page-title">⚙️ 系统设置</h2>
        <p class="page-desc">管理您的个人资料和账户安全</p>
      </div>
  
      <div class="settings-card">
        <a-tabs v-model:activeKey="activeTab">
          <!-- ==================== 个人资料 ==================== -->
          <a-tab-pane key="profile" tab="🪪 个人资料">
            <!-- 头像 -->
            <!-- 替换原来的头像区域 -->
<div class="avatar-section">
  <a-upload
    :show-upload-list="false"
    :before-upload="handleAvatarUpload"
    accept="image/*"
  >
    <div class="avatar-wrapper">
      <img :src="avatarUrl" class="avatar-img" />
      <div class="avatar-overlay">
        <camera-outlined />
        <span>更换</span>
      </div>
    </div>
  </a-upload>
  <div class="avatar-info">
    <div class="avatar-name">{{ userInfo.realName || userInfo.username }}</div>
    
<div class="avatar-role">{{ roleText }}</div>


  </div>
</div>
  
            <a-divider />
  
            <a-form :model="profileForm" layout="horizontal" :label-col="{ span: 4 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="用户名">
                <a-input :value="userInfo.username" disabled />
                <span class="form-hint">不可修改</span>
              </a-form-item>
              <a-form-item label="真实姓名">
                <a-input v-model:value="profileForm.realName" placeholder="请输入真实姓名" />
              </a-form-item>
              <a-form-item label="手机">
                <a-input v-model:value="profileForm.phone" placeholder="请输入手机号" />
              </a-form-item>
              <a-form-item label="邮箱">
                <a-input v-model:value="profileForm.email" placeholder="请输入邮箱" />
              </a-form-item>
              <a-form-item label="角色">
                <a-tag color="blue">管理员</a-tag>
                <span class="form-hint">不可修改</span>
              </a-form-item>
              <a-form-item label="注册时间">
                <span>{{ formatDate(userInfo.createTime) }}</span>
                <span class="form-hint">不可修改</span>
              </a-form-item>
              <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                <a-button type="primary" :loading="saving" @click="saveProfile">
                  <save-outlined /> 保存修改
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
  
          <!-- ==================== 修改密码 ==================== -->
          <a-tab-pane key="password" tab="🔑 修改密码">
            <a-form :model="passwordForm" layout="horizontal" :label-col="{ span: 4 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="原密码" required>
                <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入原密码" />
              </a-form-item>
              <a-form-item label="新密码" required>
                <a-input-password v-model:value="passwordForm.newPassword" placeholder="至少6位，包含字母和数字" />
              </a-form-item>
              <a-form-item label="确认密码" required>
                <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
              </a-form-item>
              <a-form-item :wrapper-col="{ offset: 4, span: 14 }">
                <a-button type="primary" :loading="pwSubmitting" @click="changePassword">
                  <lock-outlined /> 确认修改
                </a-button>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </div>
  
      <!-- 密码修改成功弹窗 -->
      <a-modal
        v-model:open="successVisible"
        :closable="false"
        :footer="null"
        :mask-closable="false"
        width="360px"
      >
        <div class="success-box">
          <check-circle-filled class="success-icon" />
          <h3>密码修改成功</h3>
          <p>请使用新密码重新登录</p>
          <a-button type="primary" block @click="relogin">重新登录</a-button>
        </div>
      </a-modal>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, onMounted,computed } from 'vue';
  import { useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { SaveOutlined, LockOutlined, CheckCircleFilled,CameraOutlined } from '@ant-design/icons-vue';
 

  

// 上传头像（转 base64 存数据库）
const handleAvatarUpload = (file) => {
  const reader = new FileReader();
  reader.onload = (e) => {
    const base64 = e.target.result;
    // 先更新本地显示
    userInfo.value.avatar = base64;
    // 调用接口保存
    saveAvatar(base64);
  };
  reader.readAsDataURL(file);
  return false; // 阻止默认上传
};

const avatarUrl = computed(() => {
  if (userInfo.value.userId) {
    return `/settings/avatar/${userInfo.value.userId}?t=${Date.now()}`;
  }
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=default`;
});

const roleText = computed(() => {
  const map = { 'ADMIN': '系统管理员', 'SUPER_ADMIN': '超级管理员' };
  return map[userInfo.value.role] || '管理员';
});

const saveAvatar = async (base64) => {
  try {
    const res = await fetch('/settings/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userInfo.value.userId,
        avatar: base64,
      }),
    });
    const result = await res.json();
    if (result.code === 200) {
      message.success('头像已更新');
      userInfo.value = { ...userInfo.value, avatar: base64 };
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
    }
  } catch (e) {}
};
  
  const router = useRouter();
  const activeTab = ref('profile');
  
  // 用户信息
  const userInfo = ref({});
  
  // 个人资料
  const profileForm = reactive({ realName: '', phone: '', email: '' });
  const saving = ref(false);
  
  // 修改密码
  const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });
  const pwSubmitting = ref(false);
  const successVisible = ref(false);
  
  onMounted(() => {
    const stored = localStorage.getItem('userInfo');
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored);
        profileForm.realName = userInfo.value.realName || '';
        profileForm.phone = userInfo.value.phone || '';
        profileForm.email = userInfo.value.email || '';
      } catch (e) {}
    }
  });
  
  // ==================== 保存个人资料 ====================
  const saveProfile = async () => {
    saving.value = true;
    try {
      const res = await fetch('/settings/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userInfo.value.userId,
          ...profileForm,
        }),
      });
      const result = await res.json();
      if (result.code === 200) {
        message.success('保存成功');
        userInfo.value = { ...userInfo.value, ...result.data };
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value));
      } else {
        message.error(result.msg || '保存失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { saving.value = false; }
  };
  
  // ==================== 修改密码 ====================
  const changePassword = async () => {
    if (!passwordForm.oldPassword) { message.warning('请输入原密码'); return; }
    if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) { message.warning('新密码至少6位'); return; }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) { message.warning('两次密码不一致'); return; }
  
    pwSubmitting.value = true;
    try {
      const res = await fetch('/settings/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userInfo.value.userId,
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword,
        }),
      });
      const result = await res.json();
      if (result.code === 200) {
        successVisible.value = true;
      } else {
        message.error(result.msg || '修改失败');
      }
    } catch (e) { message.error('网络异常'); }
    finally { pwSubmitting.value = false; }
  };
  
  const relogin = () => {
    localStorage.clear();
    router.push('/login');
  };
  
  const formatDate = (d) => d ? d.substring(0, 10) : '';
  </script>
  
  <style scoped>
  .settings-page { padding: 0; }
  .page-header { margin-bottom: 24px; }
  .page-title { font-size: 22px; font-weight: 700; color: #5C4B51; margin: 0 0 4px; }
  .page-desc { font-size: 14px; color: #A08C94; margin: 0; }
  
  .settings-card {
    background: #FFFBFD; border: 1px solid #FFE8EC; border-radius: 16px; padding: 30px;
  }
  
  .avatar-section { display: flex; align-items: center; gap: 20px; margin-bottom: 8px; }
  .avatar-name { font-size: 20px; font-weight: 700; color: #5C4B51; }
  .avatar-role { font-size: 14px; color: #A08C94; margin-top: 4px; }
  .form-hint { color: #A08C94; font-size: 12px; margin-left: 8px; }
  
  .success-box { text-align: center; padding: 16px 0; }
  .success-icon { font-size: 52px; color: #52c41a; margin-bottom: 12px; }
  .success-box h3 { margin: 0 0 8px; color: #5C4B51; }
  .success-box p { color: #A08C94; margin-bottom: 20px; }

  .avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  width: 120px;   /* 想多大改这里 */
  height: 120px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
  </style>