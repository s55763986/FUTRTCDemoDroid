package com.tencent.liteav.trtcvoiceroom.ui.room;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.blankj.utilcode.util.ToastUtils;
import com.tencent.liteav.login.model.RoomManager;
import com.tencent.liteav.trtcvoiceroom.R;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomCallback;
import com.tencent.liteav.trtcvoiceroom.model.TRTCVoiceRoomDef;
import com.tencent.liteav.trtcvoiceroom.ui.base.MemberEntity;
import com.tencent.liteav.trtcvoiceroom.ui.base.VoiceRoomSeatEntity;
import com.tencent.liteav.trtcvoiceroom.ui.list.TCConstants;
import com.tencent.liteav.trtcvoiceroom.ui.widget.CommonBottomDialog;
import com.tencent.liteav.trtcvoiceroom.ui.widget.ConfirmDialogFragment;
import com.tencent.liteav.trtcvoiceroom.ui.widget.SelectMemberView;
import com.tencent.liteav.trtcvoiceroom.ui.widget.msg.MsgEntity;
import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoiceRoomAnchorActivity extends VoiceRoomBaseActivity implements SelectMemberView.onSelectedCallback {
    public static final int ERROR_ROOM_ID_EXIT = -1301;

    private List<MemberEntity> mMemberEntityList;
    private Map<String, MemberEntity> mMemberEntityMap;
    // 用户申请上麦的map
    private Map<String, String> mTakeSeatInvitationMap;
    // 邀请人上麦的map
    private Map<String, SeatInvitation> mPickSeatInvitationMap;
    private boolean mIsEnterRoom;

    /**
     * 创建房间
     */
    public static void createRoom(Context context, String roomName, String userId,
                                  String userName, String userAvatar, String coverUrl, int audioQuality, boolean needRequest) {
        Intent intent = new Intent(context, VoiceRoomAnchorActivity.class);
        intent.putExtra(VOICEROOM_ROOM_NAME, roomName);
        intent.putExtra(VOICEROOM_USER_ID, userId);
        intent.putExtra(VOICEROOM_USER_NAME, userName);
        intent.putExtra(VOICEROOM_USER_AVATAR, userAvatar);
        intent.putExtra(VOICEROOM_AUDIO_QUALITY, audioQuality);
        intent.putExtra(VOICEROOM_ROOM_COVER, coverUrl);
        intent.putExtra(VOICEROOM_NEED_REQUEST, needRequest);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAnchor();
    }

    @Override
    public void onBackPressed() {
        if (mIsEnterRoom) {
            showExitRoom();
        } else {
            finish();
        }
    }

    private void showExitRoom() {
        if (mConfirmDialogFragment.isAdded()) {
            mConfirmDialogFragment.dismiss();
        }
        mConfirmDialogFragment.setMessage("当前正在直播，是否退出直播？");
        mConfirmDialogFragment.setNegativeClickListener(new ConfirmDialogFragment.NegativeClickListener() {
            @Override
            public void onClick() {
                mConfirmDialogFragment.dismiss();
            }
        });
        mConfirmDialogFragment.setPositiveClickListener(new ConfirmDialogFragment.PositiveClickListener() {
            @Override
            public void onClick() {
                mConfirmDialogFragment.dismiss();
                destroyRoom();
                finish();
            }
        });
        mConfirmDialogFragment.show(getFragmentManager(), "confirm_fragment");
    }

    private void destroyRoom() {
        RoomManager.getInstance().destroyRoom(mRoomId, TCConstants.TYPE_VOICE_ROOM, new RoomManager.ActionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 后台销毁房间成功");
            }

            @Override
            public void onFailed(int code, String msg) {
                Log.d(TAG, "onFailed: 后台销毁房间失败[" + code);
            }
        });
        mTRTCVoiceRoom.destroyRoom(new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    Log.d(TAG, "IM销毁房间成功");
                } else {
                    Log.d(TAG, "IM销毁房间失败:" + msg);
                }
            }
        });
        mTRTCVoiceRoom.setDelegate(null);
    }

    /**
     * 主播的逻辑
     */
    private void initAnchor() {
        mMemberEntityList = new ArrayList<>();
        mMemberEntityMap = new HashMap<>();
        mTakeSeatInvitationMap = new HashMap<>();
        mPickSeatInvitationMap = new HashMap<>();
        mVoiceRoomSeatAdapter.setEmptyText("邀请上麦");
        mVoiceRoomSeatAdapter.notifyDataSetChanged();
        mViewSelectMember.setList(mMemberEntityList);
        mViewSelectMember.setOnSelectedCallback(this);
        //刷新界面的按钮
        mBtnAudio.setActivated(true);
        mBtnMic.setActivated(true);
        mBtnEffect.setActivated(true);
        mBtnMsg.setActivated(true);

        mBtnMsg.setSelected(true);
        mBtnAudio.setSelected(true);
        mBtnMic.setSelected(true);
        mBtnEffect.setSelected(true);

        mRoomId = getRoomId();
        mCurrentRole = TRTCCloudDef.TRTCRoleAnchor;
        //设置昵称、头像
        mTRTCVoiceRoom.setSelfProfile(mUserName, mUserAvatar, null);
        RoomManager.getInstance().createRoom(mRoomId, TCConstants.TYPE_VOICE_ROOM, new RoomManager.ActionCallback() {
            @Override
            public void onSuccess() {
                internalCreateRoom();
            }

            @Override
            public void onFailed(int code, String msg) {
                if (code == ERROR_ROOM_ID_EXIT) {
                    onSuccess();
                } else {
                    ToastUtils.showLong("创建房间失败[" + code + "]:" + msg);
                    finish();
                }
            }
        });
    }

    private void internalCreateRoom() {
        final TRTCVoiceRoomDef.RoomParam roomParam = new TRTCVoiceRoomDef.RoomParam();
        roomParam.roomName = mRoomName;
        roomParam.needRequest = mNeedRequest;
        roomParam.seatCount = MAX_SEAT_SIZE;
        roomParam.coverUrl = mRoomCover;
        //        roomParam.coverUrl = ;
        mTRTCVoiceRoom.createRoom(mRoomId, roomParam, new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    mIsEnterRoom = true;
                    mToolbarTitle.setText(getString(R.string.trtcvoiceroom_main_title, roomParam.roomName, mRoomId));
                    mTRTCVoiceRoom.setAudioQuality(mAudioQuality);
                    takeMainSeat();
                    getAudienceList();
                }
            }
        });
    }

    private void getAudienceList() {
        mTRTCVoiceRoom.getUserInfoList(null, new TRTCVoiceRoomCallback.UserListCallback() {
            @Override
            public void onCallback(int code, String msg, List<TRTCVoiceRoomDef.UserInfo> list) {
                if (code == 0) {
                    for (TRTCVoiceRoomDef.UserInfo userInfo : list) {
                        if (userInfo.userId.equals(mSelfUserId)) {
                            continue;
                        }
                        MemberEntity memberEntity = new MemberEntity();
                        memberEntity.userId = userInfo.userId;
                        memberEntity.userAvatar = userInfo.userAvatar;
                        memberEntity.userName = userInfo.userName;
                        memberEntity.type = MemberEntity.TYPE_IDEL;
                        if (!mMemberEntityMap.containsKey(memberEntity.userId)) {
                            mMemberEntityMap.put(memberEntity.userId, memberEntity);
                            mMemberEntityList.add(memberEntity);
                        }
                    }
                }
            }
        });
    }

    private int getRoomId() {
        // 这里我们用简单的 userId hashcode，然后取余
        // 您的room id应该是您后台生成的唯一值
        return (mSelfUserId + "_voice_room").hashCode() & 0x7FFFFFFF;
    }

    private void takeMainSeat() {
        // 开始创建房间
        mTRTCVoiceRoom.enterSeat(0, new TRTCVoiceRoomCallback.ActionCallback() {
            @Override
            public void onCallback(int code, String msg) {
                if (code == 0) {
                    //成功上座位，可以展示UI了
                    ToastUtils.showLong("房主占座成功");
                } else {
                    ToastUtils.showLong("主播占座失败[" + code + "]:" + msg);
                }
            }
        });
    }

    /**
     * 主播点击座位列表
     *
     * @param itemPos
     */
    @Override
    public void onItemClick(final int itemPos) {
        // TODO: 2020-06-10 这里可以统一加上loading
        // 判断座位有没有人
        VoiceRoomSeatEntity entity = mVoiceRoomSeatEntityList.get(itemPos);
        if (entity.isUsed) {
            // 有人弹出禁言/踢人
            final boolean isMute = entity.isMute;
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    // 这里应该统一加上loading
                    dialog.dismiss();
                    if (position == 0) {
                        mTRTCVoiceRoom.muteSeat(changeSeatIndexToModelIndex(itemPos), !isMute, null);
                    } else {
                        mTRTCVoiceRoom.kickSeat(changeSeatIndexToModelIndex(itemPos), null);
                    }
                }
            }, isMute ? "对 Ta 解禁" : "对 Ta 禁言", "请 Ta 下麦");
            dialog.show();
        } else if (!entity.isClose) {
            // 没人弹出封麦
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    dialog.dismiss();
                    if (position == 0) {
                        if (mViewSelectMember != null) {
                            //设置一下邀请的座位号
                            mViewSelectMember.setSeatIndex(itemPos);
                            mViewSelectMember.show();
                        }
                    } else {
                        mTRTCVoiceRoom.closeSeat(changeSeatIndexToModelIndex(itemPos), true, null);
                    }
                }
            }, "邀人上麦", "封禁麦位");
            dialog.show();
        } else {
            final CommonBottomDialog dialog = new CommonBottomDialog(this);
            dialog.setButton(new CommonBottomDialog.OnButtonClickListener() {
                @Override
                public void onClick(int position, String text) {
                    dialog.dismiss();
                    if (position == 0) {
                        mTRTCVoiceRoom.closeSeat(changeSeatIndexToModelIndex(itemPos), false, null);
                    }
                }
            }, "解禁麦位");
            dialog.show();
        }
    }

    @Override
    public void onAudienceEnter(TRTCVoiceRoomDef.UserInfo userInfo) {
        super.onAudienceEnter(userInfo);
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.userId = userInfo.userId;
        memberEntity.userAvatar = userInfo.userAvatar;
        memberEntity.userName = userInfo.userName;
        memberEntity.type = MemberEntity.TYPE_IDEL;
        if (!mMemberEntityMap.containsKey(memberEntity.userId)) {
            mMemberEntityMap.put(memberEntity.userId, memberEntity);
            mMemberEntityList.add(memberEntity);
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAudienceExit(TRTCVoiceRoomDef.UserInfo userInfo) {
        super.onAudienceExit(userInfo);
        MemberEntity entity = mMemberEntityMap.remove(userInfo.userId);
        if (entity != null) {
            mMemberEntityList.remove(entity);
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAnchorEnterSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        super.onAnchorEnterSeat(index, user);
        MemberEntity entity = mMemberEntityMap.get(user.userId);
        if (entity != null) {
            entity.type = MemberEntity.TYPE_IN_SEAT;
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAnchorLeaveSeat(int index, TRTCVoiceRoomDef.UserInfo user) {
        super.onAnchorLeaveSeat(index, user);
        MemberEntity entity = mMemberEntityMap.get(user.userId);
        if (entity != null) {
            entity.type = MemberEntity.TYPE_IDEL;
        }
        if (mViewSelectMember != null) {
            mViewSelectMember.notifyDataSetChanged();
        }
    }

    @Override
    public void onAgreeClick(int position) {
        super.onAgreeClick(position);
        if (mMsgEntityList != null) {
            final MsgEntity entity = mMsgEntityList.get(position);
            String inviteId = entity.invitedId;
            if (inviteId == null) {
                ToastUtils.showLong("该请求已过期");
                return;
            }
            mTRTCVoiceRoom.acceptInvitation(inviteId, new TRTCVoiceRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        entity.type = MsgEntity.TYPE_AGREED;
                        mMsgListAdapter.notifyDataSetChanged();
                    } else {
                        ToastUtils.showShort("接受请求失败:" + code);
                    }
                }
            });
        }
    }

    @Override
    public void onReceiveNewInvitation(String id, String inviter, String cmd, String content) {
        super.onReceiveNewInvitation(id, inviter, cmd, content);
        if (cmd.equals(TCConstants.CMD_REQUEST_TAKE_SEAT)) {
            recvTakeSeat(id, inviter, content);
        }
    }

    private void recvTakeSeat(String inviteId, String inviter, String content) {
        //收到了观众的申请上麦消息，显示到通知栏
        MemberEntity memberEntity = mMemberEntityMap.get(inviter);
        MsgEntity msgEntity = new MsgEntity();
        msgEntity.userId = inviter;
        msgEntity.invitedId = inviteId;
        msgEntity.userName = (memberEntity != null ? memberEntity.userName : inviter);
        msgEntity.type = MsgEntity.TYPE_WAIT_AGREE;
        int seatIndex = Integer.parseInt(content);
        msgEntity.content = "申请上" + seatIndex + "号麦";
        mMsgEntityList.add(msgEntity);
        if (memberEntity != null) {
            memberEntity.type = MemberEntity.TYPE_WAIT_AGREE;
        }
        mTakeSeatInvitationMap.put(inviter, inviteId);
        mViewSelectMember.notifyDataSetChanged();
        mMsgListAdapter.notifyDataSetChanged();
        mRvImMsg.smoothScrollToPosition(mMsgListAdapter.getItemCount());
    }

    /**
     * mViewSelectMember 的回调函数
     * 主播选择了观众进行邀请操作
     *
     * @param seatIndex
     * @param memberEntity
     */
    @Override
    public void onSelected(int seatIndex, final MemberEntity memberEntity) {
        // 座位号 seat index 上 选择了某个用户进行邀请
        VoiceRoomSeatEntity seatEntity = mVoiceRoomSeatEntityList.get(seatIndex);
        if (seatEntity.isUsed) {
            ToastUtils.showLong("这个麦位已经有人了");
            return;
        }
        if (memberEntity.type == MemberEntity.TYPE_WAIT_AGREE) {
            //这个用户已经发过申请了，那么进行同意操作，取最后一次收到消息的情况
            String inviteId = mTakeSeatInvitationMap.get(memberEntity.userId);
            if (inviteId == null) {
                ToastUtils.showLong("该请求已过期");
                memberEntity.type = MemberEntity.TYPE_IDEL;
                mViewSelectMember.notifyDataSetChanged();
                return;
            }
            mTRTCVoiceRoom.acceptInvitation(inviteId, new TRTCVoiceRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        for (MsgEntity msgEntity : mMsgEntityList) {
                            if (msgEntity.userId != null && msgEntity.userId.equals(memberEntity.userId)) {
                                msgEntity.type = MsgEntity.TYPE_AGREED;
                                break;
                            }
                        }
                        mMsgListAdapter.notifyDataSetChanged();
                    } else {
                        ToastUtils.showShort("接受请求失败:" + code);
                        memberEntity.type = MemberEntity.TYPE_IDEL;
                        mViewSelectMember.notifyDataSetChanged();
                    }
                }
            });
            // 这里也清空一下msg list里面对应的观众信息
            for (MsgEntity msgEntity : mMsgEntityList) {
                if (msgEntity.userId == null) {
                    continue;
                }
                if (msgEntity.userId.equals(memberEntity.userId)) {
                    msgEntity.type = MsgEntity.TYPE_AGREED;
                    mTakeSeatInvitationMap.remove(msgEntity.invitedId);
                }
            }
            mMsgListAdapter.notifyDataSetChanged();
            return;
        }

        SeatInvitation seatInvitation = new SeatInvitation();
        seatInvitation.inviteUserId = memberEntity.userId;
        seatInvitation.seatIndex = seatIndex;
        String inviteId = mTRTCVoiceRoom.sendInvitation(TCConstants.CMD_PICK_UP_SEAT, seatInvitation.inviteUserId,
                String.valueOf(changeSeatIndexToModelIndex(seatIndex)), new TRTCVoiceRoomCallback.ActionCallback() {
                    @Override
                    public void onCallback(int code, String msg) {
                        if (code == 0) {
                            ToastUtils.showLong("发送邀请成功！");
                        }
                    }
                });
        mPickSeatInvitationMap.put(inviteId, seatInvitation);
        mViewSelectMember.dismiss();
    }

    @Override
    public void onCancel() {
    }

    /**
     * 观众点击拒绝邀请
     *
     * @param id
     * @param invitee
     */
    @Override
    public void onInviteeRejected(String id, String invitee) {
        super.onInviteeRejected(id, invitee);
        SeatInvitation seatInvitation = mPickSeatInvitationMap.remove(id);
        if (seatInvitation != null) {
            MemberEntity entity = mMemberEntityMap.get(seatInvitation.inviteUserId);
            if (entity != null) {
                ToastUtils.showShort(entity.userName + " 拒绝上麦");
            }
        }
    }

    /**
     * @param id
     * @param invitee
     */
    @Override
    public void onInviteeAccepted(String id, final String invitee) {
        super.onInviteeAccepted(id, invitee);
        // 抱麦的用户同意了，先获取一下之前的消息
        SeatInvitation seatInvitation = mPickSeatInvitationMap.get(id);
        if (seatInvitation != null) {
            VoiceRoomSeatEntity entity = mVoiceRoomSeatEntityList.get(seatInvitation.seatIndex);
            if (entity.isUsed) {
                Log.e(TAG, "seat " + seatInvitation.seatIndex + " already used");
                return;
            }
            mTRTCVoiceRoom.pickSeat(changeSeatIndexToModelIndex(seatInvitation.seatIndex), seatInvitation.inviteUserId, new TRTCVoiceRoomCallback.ActionCallback() {
                @Override
                public void onCallback(int code, String msg) {
                    if (code == 0) {
                        ToastUtils.showLong("抱" + invitee + "上麦成功啦！");
                    }
                }
            });
        } else {
            Log.e(TAG, "onInviteeAccepted: " + id + " user:" + invitee + " not this people");
        }
    }

    private static class SeatInvitation {
        int seatIndex;
        String inviteUserId;
    }
}
