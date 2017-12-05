package oriauto.fyx.orientedautolib.transformer;

import android.content.Context;
import android.view.View;

import oriauto.fyx.orientedautolib.utils.ScreenUtils;


/**
 * 水平偏移
 * Created by Nate on 2016/7/22.
 */
public class HorizontalStackTransformer extends HorizontalBaseTransformer {

    private Context context;
    private int spaceBetweenFirAndSecWith = 10;//第一张卡片和第二张卡片宽度差  dp单位
    private int spaceBetweenFirAndSecHeight = 10 * 2;//第一张卡片和第二张卡片高度差   dp单位
    private float pageCountAlpha = 3.0f;//显示几个相对多一个(1.0f就是两个，2.0f就是三个，就是数量加一）

    public HorizontalStackTransformer(Context context, int pageCount) {
        this.context = context;
        this.pageCountAlpha = (float) (pageCount - 1);//在这里减掉1外部不用处理
    }

    public HorizontalStackTransformer(Context context, int spaceBetweenFirAndSecWithHeight, int pageCount) {
        this.context = context;
        this.spaceBetweenFirAndSecWith = spaceBetweenFirAndSecWithHeight;
        this.spaceBetweenFirAndSecHeight = spaceBetweenFirAndSecWithHeight * 2;
        this.pageCountAlpha = (float) (pageCount - 1);//在这里减掉1外部不用处理
    }

    public HorizontalStackTransformer(Context context, int spaceBetweenFirAndSecWith, int spaceBetweenFirAndSecHeight, int pageCount) {
        this.context = context;
        this.spaceBetweenFirAndSecWith = spaceBetweenFirAndSecWith;
        this.spaceBetweenFirAndSecHeight = spaceBetweenFirAndSecHeight;
        this.pageCountAlpha = (float) (pageCount - 1);//在这里减掉1外部不用处理
    }

    @Override
    protected void onTransform(View page, float position) {
        if (position <= 0.0f) {
            page.setAlpha(1.0f);
//            Log.e("onTransform", "position <= 0.0f ==>" + position);
            page.setTranslationX(0f);
            //控制停止滑动切换的时候，只有最上面的一张卡片可以点击
            page.setClickable(true);
        } else if (position <= pageCountAlpha) {//以前用的是3.0显示了4个
//            Log.e("onTransform", "position <= 3.0f ==>" + position);
            float scale = (float) (page.getHeight() - ScreenUtils.dp2px(context, spaceBetweenFirAndSecHeight * position)) / (float) (page.getHeight());
            //控制下面卡片的可见度
            page.setAlpha(1.0f);
            //控制停止滑动切换的时候，只有最上面的一张卡片可以点击
            page.setClickable(false);
            page.setPivotX(page.getWidth() / 2f);
            page.setPivotY(page.getHeight() / 2f);
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setTranslationX(-page.getWidth() * position + (page.getWidth() * 0.5f) * (1 - scale) + ScreenUtils.dp2px(context, spaceBetweenFirAndSecWith) * position);
        }
    }
}
