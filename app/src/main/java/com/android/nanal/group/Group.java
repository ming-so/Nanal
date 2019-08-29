package com.android.nanal.group;

public class Group {
    // db table name: community
    int group_id;
    String group_name;
    int group_color;
    String account_id;


    public int getGroup_id() {
        return group_id;
    }

    public void setGroup_id(int group_id) {
        this.group_id = group_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public int getGroup_color() {
        return group_color;
    }

    public void setGroup_color(int group_color) {
        this.group_color = group_color;
    }

    public String getAccount_id() {
        return account_id;
    }

    public void setAccount_id(String account_id) {
        this.account_id = account_id;
    }

    public Group(int group_id, String group_name, int group_color, String account_id) {
        this.group_id = group_id;
        this.group_name = group_name;
        this.group_color = group_color;
        this.account_id = account_id;
    }


}
