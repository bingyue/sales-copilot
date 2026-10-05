package cn.iyque.sales;

import cn.iyque.dao.IYQueCustomerInfoDao;
import cn.iyque.domain.*;
import cn.iyque.service.IYqueConfigService;
import cn.iyque.service.impl.IYqueCustomerInfoServiceImpl;
import me.chanjar.weixin.cp.api.*;
import me.chanjar.weixin.cp.bean.external.contact.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class CustomerCallbackTest {
    IYqueCustomerInfoServiceImpl service=new IYqueCustomerInfoServiceImpl();
    IYQueCustomerInfoDao customers=mock(IYQueCustomerInfoDao.class);
    IYqueConfigService config=mock(IYqueConfigService.class);
    WxCpService cp=mock(WxCpService.class);
    WxCpExternalContactService api=mock(WxCpExternalContactService.class);
    @BeforeEach void setup() throws Exception {
        ReflectionTestUtils.setField(service,"iyQueCustomerInfoDao",customers); ReflectionTestUtils.setField(service,"iYqueConfigService",config);
        when(config.findWxcpservice()).thenReturn(cp); when(cp.getExternalContactService()).thenReturn(api);
        var external=new ExternalContact(); external.setExternalUserId("customer"); external.setName("新名称");
        var followed=new FollowedUser(); followed.setUserId("employee"); followed.setCreateTime(1700000000L);
        var result=new WxCpExternalContactInfo(); result.setExternalContact(external); result.setFollowedUsers(List.of(followed));
        when(api.getExternalContact("customer")).thenReturn(result);
        when(customers.saveAllAndFlush(any())).thenAnswer(inv->inv.getArgument(0));
    }
    @Test void ordinaryCallbackWithoutChannelPersistsCustomer() {
        var callback=new IYqueCallBackBaseMsg(); callback.setExternalUserID("customer"); callback.setUserID("employee");
        service.addCustomerCallBackAction(callback);
        verify(customers).saveAllAndFlush(argThat(values->{ var row=values.iterator().next(); return row.getEId().equals("customer&employee")&&row.getCustomerName().equals("新名称"); }));
    }
    @Test void refreshingCustomerPreservesLocalMetadata() {
        var old=new IYQueCustomerInfo(); old.setTagIds("tag-1"); old.setAvatar("avatar-url"); old.setState("existing-channel");
        when(customers.findByExternalUseridAndUserId("customer","employee")).thenReturn(old);
        var row=service.saveCustomer("customer").get(0);
        assertThat(row.getTagIds()).isEqualTo("tag-1"); assertThat(row.getAvatar()).isEqualTo("avatar-url"); assertThat(row.getState()).isEqualTo("existing-channel");
    }
    @Test void baseFailureIsPropagatedForRetry() throws Exception {
        when(api.getExternalContact("customer")).thenThrow(new IllegalStateException("network"));
        assertThatThrownBy(()->service.saveCustomer("customer")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(customers,never()).saveAllAndFlush(any());
    }
}
