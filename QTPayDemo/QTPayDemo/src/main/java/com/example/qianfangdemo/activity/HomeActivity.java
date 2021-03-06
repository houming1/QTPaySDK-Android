package com.example.qianfangdemo.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.qianfangdemo.Utils.T;
import com.example.qianfangdemo.Utils.Utils;
import com.example.qianfangdemo.base.ConstValue;
import com.example.qianfangdemo.fragment.HomeFragment;
import com.example.qianfangdemo.fragment.MyInfoFragment;
import com.qfpay.sdk.activity.CashierActivity;
import com.qfpay.sdk.common.QTCallBack;
import com.qfpay.sdk.common.QTConst;
import com.qfpay.sdk.common.QTPayCommon;
import com.qfpay.sdk.entity.Coupon;
import com.qfpay.sdk.entity.CustomerInfo;
import com.qfpay.sdk.entity.ExtraInfo;
import com.qfpay.sdk.entity.Good;
import com.qfpay.sdk.entity.QTHolder;

import java.util.List;
import java.util.Map;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;
import qfpay.wxshop.R;

public class HomeActivity extends FragmentActivity{

	private VerticalViewPager viewPager;

	private Dialog dialog;

	// private int specialAmt;

	private Fragment homeFragment, InfoFragment;

	List<Good> goods;
	List<ExtraInfo> extraInfo;

	protected RequestQueue mQueue;
	protected QTPayCommon mqt;



	/**
	 * 外部订单号
	 */
	public String mOutSn;

	private String orderToken;

	private int specialAmt;
	QTHolder holder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_page);

		viewPager = (VerticalViewPager) findViewById(R.id.pager);
		findViewById(R.id.my_account).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onAccountClick();
			}
		});
		mQueue = Volley.newRequestQueue(getApplicationContext());
		mqt = QTPayCommon.getInstance(getApplicationContext());

		dialog = new Dialog(HomeActivity.this, R.style.DialogStyle);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		dialog.setContentView(R.layout.my_dialog);
		dialog.setCancelable(false);

		holder = (QTHolder) getIntent().getSerializableExtra(QTConst.EXTRO);

		initView();

		homeFragment = new HomeFragment();
		InfoFragment = new MyInfoFragment();

		specialAmt = ((QTHolder) getIntent().getSerializableExtra(QTConst.EXTRO)).getTotalAmt();
		if (specialAmt < 0) {
			finish();
			return;
		}

		if (TextUtils.isEmpty(ConstValue.orderToken)) {
			Toast.makeText(getApplicationContext(), "缺少订单token", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		this.orderToken = ConstValue.orderToken;

	}

	private void onAccountClick() {

		mqt.getCustomerInfo(specialAmt + "", new int[]{QTConst.CustomerInfo_Coupon}, new QTCallBack() {
			@Override
			public void onSuccess(Map<String, Object> dataInfo) {
				if (dataInfo.containsKey("customer_info")) {
					CustomerInfo mCustomerInfo = (CustomerInfo) dataInfo.get("customer_info");
					final List<Coupon> coupons = mCustomerInfo.getCoupons();
					String[] couponTitle = new String[coupons.size()];
					for (int i = 0; i< coupons.size(); i++){
						couponTitle[i] = coupons.get(i).getContent()+ "  金额 :" + coupons.get(i).getAmt();
					}

					AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
					builder.setTitle("选择优惠券");
					builder.setItems(couponTitle, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							holder.setCoupon_code(coupons.get(which).getCoupon_code());
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}

			}

			@Override
			public void onError(Map<String, String> errorInfo) {

			}
		});

	}

	private void initView() {
		viewPager.setAdapter(new MyFragmentAdapter(getSupportFragmentManager()));
	}

	private class MyFragmentAdapter extends FragmentPagerAdapter {

		public MyFragmentAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
			case 0:
				return homeFragment;
			case 1:
				return InfoFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

	}

	/**
	 * 请求配置参数
	 * 
	 */
	public void getSettingConfig() {

		if (!Utils.isCanConnectionNetWork(HomeActivity.this)) {
			Toast.makeText(HomeActivity.this, "网络连接异常！", Toast.LENGTH_SHORT);
			return;
		}

		if (dialog != null) {
			dialog.show();
		}

		mqt.getSettingConfiguration(orderToken, new QTCallBack() {
			@Override
			public void onSuccess(Map<String, Object> dataMap) {
				if (dialog.isShowing()) {
					dialog.dismiss();
				}
				ConstValue.paymentType = dataMap;
				JumpTo();
			}

			@Override
			public void onError(Map<String, String> errorInfo) {
				if (dialog.isShowing()) {
					dialog.dismiss();
				}

				AlertDialog dialog = new AlertDialog.Builder(HomeActivity.this).setMessage("获取请求参数失败")
						.setPositiveButton("退出", new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								finish();
							}
						}).setNegativeButton("重试", new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								getSettingConfig();
							}
						}).show();
				dialog.setCancelable(false);

				Toast.makeText(HomeActivity.this, "请求配置参数失败", Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void JumpTo() {
		Intent intent = new Intent(HomeActivity.this, CashierActivity.class);
		intent.putExtra(QTConst.EXTRO,holder);
		startActivityForResult(intent, ConstValue.REQUEST_FOR_CASHIER);
		overridePendingTransition(R.anim.qt_slide_in_from_bottom, R.anim.qt_slide_out_to_top);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		T.d("setResult成功！");

		if (requestCode == ConstValue.REQUEST_FOR_CASHIER) {

			if (resultCode == Activity.RESULT_OK) {
				int result = data.getExtras().getInt("pay_result");

				T.d("result = " + result);
				switch (result) {
				case QTConst.PAYMENT_RETURN_SUCCESS:
					T.d("交易成功！关闭该订单页面");
					startActivity(new Intent(HomeActivity.this, PaymentResultActivity.class));
					
					break;

				case QTConst.PAYMENT_RETURN_FAIL:
					break;
				}
			} else if (resultCode == Activity.RESULT_CANCELED) {
				T.d("取消");

			} else if (resultCode == QTConst.ACTIVITY_RETURN_ERROR) {
			}

		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// super.onSaveInstanceState(outState);
	}

}
