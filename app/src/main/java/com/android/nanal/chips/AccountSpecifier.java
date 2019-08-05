package com.android.nanal.chips;

import android.accounts.Account;

/**
 * The AccountSpecificAdapter interface describes an Adapter
 * that can take an account to retrieve information tied to
 * a specific account.
 */
public interface AccountSpecifier {
    public void setAccount(Account account);
}

