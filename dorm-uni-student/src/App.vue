<script>
export default {
	
	 globalData: {
	    noticeId: null
	  },
  onLaunch() {
    const token = uni.getStorageSync('token');
    const userInfo = uni.getStorageSync('userInfo');
    
    if (!token) {
      uni.reLaunch({ url: '/pages/login/login' });
      return;
    }

    // 解析角色
    try {
      const user = JSON.parse(userInfo);
      if (user.role !== 'STUDENT') {
        uni.clearStorageSync();
        uni.reLaunch({ url: '/pages/login/login' });
      }
    } catch (e) {
      uni.clearStorageSync();
      uni.reLaunch({ url: '/pages/login/login' });
    }
  }
};
</script>