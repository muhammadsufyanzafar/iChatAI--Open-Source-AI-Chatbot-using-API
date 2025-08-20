package com.zafar.ichatai;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public class SimpleTextWatcher implements TextWatcher {
    private final Consumer<CharSequence> onChange;
    public SimpleTextWatcher(Consumer<CharSequence> onChange) { this.onChange = onChange; }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.accept(s); }
    @Override public void afterTextChanged(Editable s) {}
}
