package com.android.nanal.interfaces;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

/*
 * Interface for additional options in the AllInOne menu.
 * AllInOne 메뉴의 추가 옵션에 대한 인터페이스
 */
public interface AllInOneMenuExtensionsInterface {
    /**
     * Returns additional options.
     * 추가 옵션을 반환
     */
    Integer getExtensionMenuResource(Menu menu);

    /**
     * Handle selection of the additional options.
     * 추가 옵션 선택을 처리함
     */
    boolean handleItemSelected(MenuItem item, Context context);
}
