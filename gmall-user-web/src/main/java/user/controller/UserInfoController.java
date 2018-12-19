package user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.houfan.gmall.bean.UserInfo;
import com.houfan.gmall.service.UserInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserInfoController {

    // dubbo中的消费者端注解,可以利用代理来完成服务端接口对象的注入
    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("/updateUserInfo")
    public ResponseEntity updateUserInfo(){
        UserInfo userInfo = new UserInfo();
        userInfo.setLoginName("李四");
        userInfo.setId(1);
        userInfoService.updateUser(userInfo);

        return ResponseEntity.ok("SUCCESS");
    }

    /**
     * 增加用户
     */
    @RequestMapping("/addUserInfo")
    public ResponseEntity addUser(UserInfo userInfo){

        userInfoService.addUser(userInfo);

        return ResponseEntity.ok("SUCCESS");
    }

    /**
     *  获取所有的用户信息
     */
    @RequestMapping("/allUserInfo")
    public ResponseEntity<List<UserInfo>> getAllUserInfo(){
        List<UserInfo> userInfoListAll = userInfoService.getUserInfoListAll();

        return ResponseEntity.ok(userInfoListAll);
    }

    /**
     * 获取所有的用户和他们的收货地址
     */
    @RequestMapping("/getAllUserAndAddress")
    public ResponseEntity<List<UserInfo>> getAllUserAndAddress(){
        List<UserInfo> userInfoList = userInfoService.getAllUserAndAddress();

        return ResponseEntity.ok(userInfoList);

    }

}
