package com.fyx.oriauto;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import oriauto.fyx.orientedautolib.OrientedAutoViewPager;
import oriauto.fyx.orientedautolib.transformer.HorizontalStackTransformer;

public class MainActivity extends AppCompatActivity {

    private OrientedAutoViewPager viewPager;
    private List<Fragment> fragmentList = new ArrayList<>(); // ViewPager包含的页面列表，一般给adapter传的是一个list
    private ContentFragmentAdapter pageViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }


    private void initView() {
        viewPager = (OrientedAutoViewPager) findViewById(R.id.src_view_pager);
    }

    private void initData() {
        List<Fragment> fragments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fragments.add(CardFragment.newInstance(i));
        }
        initPageData(fragments);
    }

    private void initPageData(final List<Fragment> cardFragments) {
        //第一步，叠图计算,获取循环数据
        List<Fragment> frgs = getLoopData(cardFragments);
        pageViewAdapter = new ContentFragmentAdapter(getSupportFragmentManager(), frgs);
        viewPager.setAdapter(pageViewAdapter);
        viewPager.setOffscreenPageLimit(cardFragments.size());
        viewPager.setCurrentItem(1);//第二步，如果循环设置为1
        viewPager.setAutoLoop(true);//第三步，启动自动滚动播放
        viewPager.setAutoScroDelayMillis(3000);//第四步，设置自动滚动播放间隔时间,默认两秒
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //第五步，配合叠图计算,跳转对应位置这句话要放在最上面,放在OnPageChangeListener里也行
                viewPager.setPageScrolled(position, positionOffset, positionOffsetPixels);
                // TODO: 2017/12/4 下面做要做的工作
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //第五步，配合叠图计算,跳转对应位置这句话要放在最上面,放在放在SimpleOnPageChangeListener里也行
                viewPager.setPageScrolled(position, positionOffset, positionOffsetPixels);
                // TODO: 2017/12/4 下面做要做的工作
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //垂直翻滚，还是水平翻滚,默认水平翻滚
//        viewPager.setOrientation(FyxViewPager.Orientation.VERTICAL);
//        viewPager.setPageTransformer(true, new VerticalStackTransformer(this, cardFragments.size()));
        //水平翻滚
        viewPager.setPageTransformer(true, new HorizontalStackTransformer(this, 10, cardFragments.size()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewPager.stopLoop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewPager.startLoop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewPager.stopLoop();
    }

    /**
     * 为了避免fragment already added ,要重新实例化
     * 为了让最后一位能看见叠图，此处算法首位为原来的末位，
     * 滑动到首位会设置自动跳转到中间的末位，
     * 滑动到末位下一位会跳转到标准的第一位
     *
     * @param cardFragments
     */
    public List<Fragment> getLoopData(List<Fragment> cardFragments) {
        if (cardFragments != null) {
            //====================叠图计算方法=============
            fragmentList.clear();
            //加上末位放在第一个位置，为了能左滑
            fragmentList.add(CardFragment.newInstance(cardFragments.size() - 1));
            //加上标准展示数据
            for (int i = 0; i < cardFragments.size(); i++) {
                fragmentList.add(CardFragment.newInstance(i));
            }
            //重复加一遍为了末位图后有叠图，否则后几位叠图不全
            for (int i = 0; i < cardFragments.size(); i++) {
                fragmentList.add(CardFragment.newInstance(i));
            }
            return fragmentList;
            //====================叠图计算方法=============
        }
        return fragmentList;
    }

}
