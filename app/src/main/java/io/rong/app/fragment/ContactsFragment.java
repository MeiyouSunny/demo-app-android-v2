package io.rong.app.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.activity.NewFriendListActivity;
import io.rong.app.activity.PersonalDetailActivity;
import io.rong.app.activity.MainActivity;
import io.rong.app.activity.PublicServiceActivity;
import io.rong.app.adapter.ContactsMultiChoiceAdapter;
import io.rong.app.adapter.FriendListAdapter;
import io.rong.app.model.Friend;
import io.rong.app.ui.DePinnedHeaderListView;
import io.rong.app.ui.DeSwitchGroup;
import io.rong.app.ui.DeSwitchItemView;
import io.rong.app.utils.Constants;
import io.rong.imkit.RongIM;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by Bob on 2015/3/26.
 */
@SuppressWarnings("ALL")
public class ContactsFragment extends Fragment implements DeSwitchGroup.ItemHander, View.OnClickListener, TextWatcher, FriendListAdapter.OnFilterFinished, AdapterView.OnItemClickListener {

    private static final String TAG = ContactsFragment.class.getSimpleName();
    protected ContactsMultiChoiceAdapter mAdapter;
    private DePinnedHeaderListView mListView;
    private DeSwitchGroup mSwitchGroup;
    /**
     * 好友list
     */
    protected List<Friend> mFriendsList;
    private TextView textViwe;
    private ReceiveMessageBroadcastReciver mBroadcastReciver;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.de_list_address, null);

        mListView = (DePinnedHeaderListView) view.findViewById(R.id.de_ui_friend_list);
        mSwitchGroup = (DeSwitchGroup) view.findViewById(R.id.de_ui_friend_message);

        mListView.setPinnedHeaderView(LayoutInflater.from(this.getActivity()).inflate(R.layout.de_item_friend_index,
                mListView, false));
        //TODO
        textViwe = (TextView) mListView.getPinnedHeaderView();

        mListView.setFastScrollEnabled(false);

        mListView.setOnItemClickListener(this);
        mSwitchGroup.setItemHander(this);

        mListView.setHeaderDividersEnabled(false);
        mListView.setFooterDividersEnabled(false);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.ACTION_DMEO_AGREE_REQUEST);
        if (mBroadcastReciver == null) {
            mBroadcastReciver = new ReceiveMessageBroadcastReciver();
        }
        getActivity().registerReceiver(mBroadcastReciver, intentFilter);

        return view;
    }
    private class ReceiveMessageBroadcastReciver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainActivity.ACTION_DMEO_AGREE_REQUEST)) {

                updateDate();
            }
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {


        ArrayList<UserInfo> userInfos = null;

        //获取好友列表
        if (DemoContext.getInstance() != null) {
            userInfos = DemoContext.getInstance().getFriends();
        }
        mFriendsList = new ArrayList<Friend>();

        if (userInfos != null) {
            for (UserInfo userInfo : userInfos) {
                Friend friend = new Friend();
                friend.setNickname(userInfo.getName());
                friend.setPortrait(userInfo.getPortraitUri() + "");
                friend.setUserId(userInfo.getUserId());
                mFriendsList.add(friend);
            }
        }
        mFriendsList = sortFriends(mFriendsList);
//        mFriendsList.get(0).getSearchKey();
        mAdapter = new ContactsMultiChoiceAdapter(getActivity(), mFriendsList);
        mListView.setAdapter(mAdapter);

        fillData();

        super.onViewCreated(view, savedInstanceState);
    }


    private final void fillData() {

//        mAdapter.removeAll();
        mAdapter.setAdapterData(mFriendsList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {

        if (v instanceof DeSwitchItemView) {
            CharSequence tag = ((DeSwitchItemView) v).getText();

            if (mAdapter != null && mAdapter.getSectionIndexer() != null) {
                Object[] sections = mAdapter.getSectionIndexer().getSections();
                int size = sections.length;

                for (int i = 0; i < size; i++) {
                    if (tag.equals(sections[i])) {
                        int index = mAdapter.getPositionForSection(i);
                        mListView.setSelection(index + mListView.getHeaderViewsCount());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object tagObj = view.getTag();
        Log.e(TAG, Constants.DEBUG + "--------fragment----onItemClick------------");

        if (tagObj != null && tagObj instanceof ContactsMultiChoiceAdapter.ViewHolder) {
            ContactsMultiChoiceAdapter.ViewHolder viewHolder = (ContactsMultiChoiceAdapter.ViewHolder) tagObj;
            String friendId = viewHolder.friend.getUserId();
            if (friendId == "★001") {
                Intent intent = new Intent(getActivity(), NewFriendListActivity.class);
                startActivityForResult(intent,20);
            } else if (friendId == "★002") {
                if (RongIM.getInstance() != null) {
                    RongIM.getInstance().startSubConversationList(getActivity(), Conversation.ConversationType.GROUP);
                }
            } else if (friendId == "★003") {
                Intent intent = new Intent(getActivity(), PublicServiceActivity.class);
                getActivity().startActivity(intent);
            } else {
                Intent intent = new Intent(getActivity(), PersonalDetailActivity.class);
                intent.putExtra("PERSONAL", viewHolder.friend.getUserId());
                startActivityForResult(intent, 19);
            }
            return;
        }

    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
        super.onDestroyView();
    }

    /**
     * 好友数据排序
     *
     * @param friends 好友 List
     * @return 排序后的好友 List
     */
    private ArrayList<Friend> sortFriends(List<Friend> friends) {

        String[] searchLetters = getResources().getStringArray(R.array.de_search_letters);

        HashMap<String, ArrayList<Friend>> userMap = new HashMap<String, ArrayList<Friend>>();

        ArrayList<Friend> friendsArrayList = new ArrayList<Friend>();

        for (Friend friend : friends) {

            String letter = new String(new char[]{friend.getSearchKey()});

            if (userMap.containsKey(letter)) {
                ArrayList<Friend> friendList = userMap.get(letter);
                friendList.add(friend);

            } else {
                ArrayList<Friend> friendList = new ArrayList<Friend>();
                friendList.add(friend);
                userMap.put(letter, friendList);
            }

        }
        ArrayList<Friend> friendList = new ArrayList<Friend>();
        friendList.add(new Friend("★001", "新的朋友", getResources().getResourceName(R.drawable.de_address_new_friend)));
        friendList.add(new Friend("★002", "群聊",getResources().getResourceName(R.drawable.de_address_group) ));
        friendList.add(new Friend("★003", "公众号", getResources().getResourceName(R.drawable.de_address_public)));
        userMap.put("★", friendList);
        for (int i = 0; i < searchLetters.length; i++) {
            String letter = searchLetters[i];
            ArrayList<Friend> fArrayList = userMap.get(letter);
            if (fArrayList != null) {
                friendsArrayList.addAll(fArrayList);
            }
        }

        return friendsArrayList;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, Constants.DEBUG + "-----onActivityResult-resultCode---");
        if (resultCode == Constants.DELETE_USERNAME_REQUESTCODE) {
            Log.e(TAG, Constants.DEBUG + "-----onActivityResult-resultCode---" + resultCode);
            updateDate();
        }

        if(requestCode == 20){
            Log.e(TAG, Constants.DEBUG + "-----onActivityResult-requestCode---" + requestCode);
            updateDate();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateDate() {
        if(mAdapter !=null){
            mAdapter = null;
        }
        ArrayList<UserInfo> userInfos = null;
        //获取好友列表
        if (DemoContext.getInstance() != null) {
            userInfos = DemoContext.getInstance().getFriends();
        }
        mFriendsList = new ArrayList<Friend>();

        if (userInfos != null) {
            for (UserInfo userInfo : userInfos) {
                Friend friend = new Friend();
                friend.setNickname(userInfo.getName());
                friend.setPortrait(userInfo.getPortraitUri() + "");
                friend.setUserId(userInfo.getUserId());
                mFriendsList.add(friend);
            }
        }
        mFriendsList = sortFriends(mFriendsList);
//        mFriendsList.get(0).getSearchKey();
        mAdapter = new ContactsMultiChoiceAdapter(getActivity(), mFriendsList);

        mListView.setAdapter(mAdapter);
        fillData();
    }

    @Override
    public void onDestroy() {
        if (mBroadcastReciver != null) {
            getActivity().unregisterReceiver(mBroadcastReciver);
        }
        super.onDestroy();
    }

    @Override
    public void onFilterFinished() {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }


}
