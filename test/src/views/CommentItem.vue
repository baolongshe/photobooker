<template>
  <div class="comment-item">
    <img class="comment-avatar" :src="comment.avatar || defaultAvatar" alt="avatar" @error="onAvatarError($event)" />
    <div class="comment-header">
      <span class="comment-user">{{ comment.userName }}</span>
      <span class="comment-time">{{ formatDate(comment.createTime) }}</span>
      <el-button type="text" @click="showReply(comment)">回复</el-button>
      <el-button v-if="userStore.user?.id === comment.userId" type="text" @click="deleteComment(comment.id)">删除</el-button>
    </div>
    <div class="comment-content" v-html="highlightAt(comment.content)"></div>
    <!-- 回复框 -->
    <div v-if="replyingTo === comment.id" class="reply-box">
      <el-input v-model="localReplyContent" type="textarea" :rows="1" :placeholder="'回复 @' + comment.userName" />
      <el-button type="primary" size="small" @click="submitReplyLocal(comment)">发送</el-button>
      <el-button size="small" @click="cancelReply">取消</el-button>
    </div>
    <!-- 递归渲染子评论 -->
    <div class="comment-children" v-if="comment.children && comment.children.length">
      <CommentItem
        v-for="child in comment.children"
        :key="child.id"
        :comment="child"
        :replyingTo="replyingTo"
        :replyContent="replyContent"
        :defaultAvatar="defaultAvatar"
        @show-reply="showReply"
        @submit-reply="(...args) => $emit('submit-reply', ...args)"
        @cancel-reply="cancelReply"
        @delete-comment="deleteComment"
        @on-avatar-error="onAvatarError"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, ref, watch } from 'vue'
import { useUserStore } from '../stores/user'
import CommentItem from './CommentItem.vue'

const props = defineProps({
  comment: { type: Object, required: true },
  replyingTo: { type: Number, required: false },
  replyContent: { type: String, required: false },
  defaultAvatar: { type: String, required: false }
})
const emit = defineEmits(['show-reply', 'submit-reply', 'cancel-reply', 'delete-comment', 'on-avatar-error'])

const userStore = useUserStore()

const localReplyContent = ref('')
// 当replyingTo变化且不是当前评论，清空本地内容
watch(() => props.replyingTo, (val) => {
  if (val !== props.comment.id) localReplyContent.value = ''
})

const showReply = (comment: any) => emit('show-reply', comment)
const submitReplyLocal = (comment: any) => {
  emit('submit-reply', comment, localReplyContent.value)
  localReplyContent.value = ''
}
const cancelReply = () => emit('cancel-reply')
const deleteComment = (id: number) => emit('delete-comment', id)
const onAvatarError = (e: Event) => emit('on-avatar-error', e)

const formatDate = (dateStr: string) => {
  return dateStr ? dateStr.substring(0, 10) : ''
}
const highlightAt = (content: string) => {
  return content.replace(/@([\w\u4e00-\u9fa5]+)/g, '<span class="at-user">@$1</span>')
}
</script>

<style scoped>
.comment-item {
  position: relative;
  margin-bottom: 18px;
  padding: 16px 16px 8px 56px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(64,158,255,0.04);
  border: 1px solid #f0f0f0;
  min-height: 56px;
}

.comment-avatar {
  position: absolute;
  left: 16px;
  top: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6f0fa;
  object-fit: cover;
  border: 1px solid #dbeafe;
}

.comment-children {
  margin-left: 40px;
  border-left: 2px solid #e6f0fa;
  padding-left: 16px;
  margin-top: 10px;
  background: #f8fbff;
  border-radius: 6px;
  padding-bottom: 6px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  color: #222;
  margin-bottom: 2px;
}

.comment-user {
  font-weight: bold;
  color: #409EFF;
  font-size: 16px;
}

.comment-time {
  color: #b0b0b0;
  font-size: 13px;
  margin-left: 4px;
}

.comment-content {
  margin: 8px 0 10px 0;
  font-size: 16px;
  color: #222;
  line-height: 1.7;
  word-break: break-all;
}

.reply-box {
  margin: 10px 0 0 0;
  display: flex;
  gap: 8px;
}

.el-button[type='text'] {
  color: #409EFF;
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
}
.el-button[type='text']:hover {
  color: #fff;
  background: #409EFF;
}

.at-user {
  color: #409EFF;
  font-weight: bold;
  background: #e6f0fa;
  padding: 0 2px;
  border-radius: 2px;
}
</style> 