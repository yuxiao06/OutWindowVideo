package com.demo.outwindowvideo;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @since 2022/8/15
 */
public class FakeCommentUtil {

    public static void addFakeComment(Context context, ViewGroup viewGroup) {
        viewGroup.addView(createFakeCommentItem(context, "我迷路了", "出框视频效果"));
        viewGroup.addView(createFakeCommentItem(context, "韩偓", "辛夷才谢小桃发，蹋青过后寒食前。四时最好是三月，一去不回唯少年。"));
        viewGroup.addView(createFakeCommentItem(context, "张旭", "欲寻轩槛列清尊，江上烟云向晚昏。须倩东风吹散雨，明朝却待入华园。"));
        viewGroup.addView(createFakeCommentItem(context, "王维", "空山新雨后，天气晚来秋"));
    }

    private static View createFakeCommentItem(Context context, String nickName, String content) {
        TextView textView = new TextView(context);
        textView.setTextColor(Color.parseColor("#000000"));
        textView.setTextSize(COMPLEX_UNIT_SP, 16);
        textView.setPadding(28, 21, 28, 21);

        SpannableString ss = new SpannableString(nickName + ": " + content);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#5b6c92")), 0, nickName.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);

        return textView;
    }
}
