package com.thinkgem.jeesite.modules.zb.web;

import com.alibaba.fastjson.JSON;
import com.thinkgem.jeesite.common.persistence.Page;
import com.thinkgem.jeesite.common.web.BaseController;
import com.thinkgem.jeesite.modules.sys.entity.User;
import com.thinkgem.jeesite.modules.sys.utils.UserUtils;
import com.thinkgem.jeesite.modules.zb.entity.*;
import com.thinkgem.jeesite.modules.zb.service.ZbInformationService;
import com.thinkgem.jeesite.modules.zb.service.ZbzfService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author: liangjie.Hang
 **/

@Controller
@RequestMapping(value = "${adminPath}/zb/zb_zf/zb_zf")
public class ZbzfController extends BaseController {

    @Autowired
    private ZbzfService zbzfService;
    @Autowired
    private ZbInformationService zbInformationService;

    /**
     * 查数据跳转项目台账页面
     * @param zbzf
     * @param request
     * @param response
     * @param model
     * @return
     */
    @RequiresPermissions("zb_zbInfo:ZbInformationForm:view")
    @RequestMapping(value = {"zfList"})
    public String gozfList(Zbzf zbzf, HttpServletRequest request, HttpServletResponse response, Model model){
        if (zbzf.getApprovalProgress() == null) {
            zbzf.setApprovalProgress("未报");
        }
        this.selectZfList(zbzf,request,response,model);
        return "modules/zb/zb_zf/zbzfList";
    }

    /**
     * 查数据 跳转编辑页面
     * @param zbzf
     * @param request
     * @param response
     * @param model
     * @return
     */
    @RequiresPermissions("zb_zbInfo:ZbInformationForm:view")
    @RequestMapping(value = {"goUpdateZfList"})
    public String goUpdateZfList(Zbzf zbzf, HttpServletRequest request, HttpServletResponse response, Model model){
        this.selectZfList(zbzf,request,response,model);
        return "modules/zb/zb_zf/updatezbzfList";
    }

    /**
     * 查招标支付数据
     * @param zbzf
     * @param request
     * @param response
     * @param model
     */
    public void  selectZfList(Zbzf zbzf, HttpServletRequest request, HttpServletResponse response, Model model){
        zbzf.setType(request.getParameter("type"));
        if(zbzf.getTjfzr()==""){
            zbzf.setTjfzr(request.getParameter("tjfzr"));
        }
        zbzf=zbzfService.ZbUserInfor(zbzf);
        Page<Zbzf>  page=null;
        if("查询全部".equals(zbzf.getSubmit())){
            zbzf.setSubmit("全部");
            page = zbzfService.selectZbzf(zbzf,new Page<Zbzf>(request,response));
        }  else {
            zbzf.setSsbm(request.getParameter("ssbm"));
            zbzf.setJxrenssbm(request.getParameter("jxrenssbm"));
            page = zbzfService.selectZbzf(zbzf,new Page<Zbzf>(request,response,50));
        }
        model.addAttribute("page", page);
        model.addAttribute("zbzf", zbzf);
    }



    @ResponseBody
    @RequestMapping(value = "getOfficeName")
    public String getOfficeName(HttpServletRequest request) {
        String officeid =  request.getParameter("officeid");
        String  ssbm =zbzfService.getOfficeName(officeid);
        return ssbm;
    }


    /**
     * 修改或者保存
     * @param redirectAttributes
     * @param zbzfJxMoney
     * @param zbJxRen
     * @param request
     * @return
     */
    @RequestMapping(value = {"updateToOneList"})
    @ResponseBody
    public String updateList(RedirectAttributes redirectAttributes, ZbJxMoney zbzfJxMoney, ZbJxRen zbJxRen, HttpServletRequest request,ZbZfLog zbZfLog) {
        String jsonXtgg = request.getParameter("jsonXtgg");
        String sta = request.getParameter("sta");
         List<ZbJxMoney> zbzfJxMoneyList = JSON.parseArray(jsonXtgg, ZbJxMoney.class);
        int up=0,in =0,i;
         for (i=0; i < zbzfJxMoneyList.size(); i++) {
            zbzfJxMoney = zbzfJxMoneyList.get(i);
            if(zbzfJxMoney.getSjwjq().equals("未结清")){
                zbzfJxMoney.setSjwjq("-400");
            }else {
                zbzfJxMoney.setSjwjq(null);
            }
            zbzfJxMoney=zbzfService.ZbUserInfor2(zbzfJxMoney);
            /*去获取旧数据*/
            ZbJxMoney zbJxOldData = zbzfService.get(zbzfJxMoney);
             zbzfService.updateSta(zbzfJxMoney.getXmbh(),sta);
            if(!"".equals(zbzfJxMoney.getJxid()) && zbzfJxMoney.getJxid() !=0){
                /*根据id获取数据*/
                zbzfService.update(zbzfJxMoney);
                String jxjd=zbzfJxMoney.getJxjxjd();
                String projectnumber=zbzfJxMoney.getXmbh();
                zbzfService.updateZfJxjd(jxjd,projectnumber);
                zbZfLog.setOperationType("修改");
                up+=1;
                zbInformationService.insertZbInquirylog("编号：" + zbzfJxMoney.getJxid(), "修改");
            }else {
                zbzfJxMoney.setXzr(UserUtils.getUser().getName());
                zbzfService.insert1(zbzfJxMoney);
                zbZfLog.setOperationType("新增");
                in+=1;
            }
            /*循环jx人修改 或者新增   大部分没有分配人*/
            String renJson = request.getParameter("renJson");
            if(renJson!=null){
                List<ZbJxRen> zbzfJxrenList = JSON.parseArray(renJson, ZbJxRen.class);
                int s = 0;
                for (int j=0; j<zbzfJxrenList.size();j++) {
                    zbJxRen = zbzfJxrenList.get(j);
                    /*     拿id 获取部门名字 不合理弃用*/
                    String  ssbm =zbzfService.getOfficeName(zbJxRen.getJxrenssbmid());
                    zbJxRen.setJxrenssbm(ssbm);
                    if( !"".equals(zbJxRen.getRenid()) && zbJxRen.getRenid() !=0){
                        try {
                            s = zbzfService.updateZbJxRen(zbJxRen);
                            if (s < 0) {
                                throw new RuntimeException("对不起，错误");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            addMessage(redirectAttributes, "修改绩效人金额信息失败!");
                        }
                    }else {
                        zbJxRen.setJxuid(zbzfJxMoney.getJxid());//另一张表绩效钱的id
                        s = zbzfService.insertZbJxRen(zbJxRen);
                    }
                }
            }

            /* 1.获取操作类型
                2.获取jx中的数据赋值log
                3.调用log
                */
            if(zbJxOldData!=null){
                zbZfLog.setTableDataId(""+zbJxOldData.getJxid());
                zbZfLog.setImplementContent(
                        "绩效季度 : "+zbJxOldData.getJxjxjd()+
                        "\n难度系数后A : "+zbJxOldData.getNdxsq()+
                        "\n标准金类型B : "+zbJxOldData.getBzj()+
                        "\n标准金额B : "+zbJxOldData.getBzjq()+
                        "\n多项目系数C : "+zbJxOldData.getDxmxsq()+
                        "\n最终绩效D : "+zbJxOldData.getZzjxq()+
                        "\n难度系数E : "+zbJxOldData.getNdxs()+
                        "\n多项目系数F : "+zbJxOldData.getDxmxs()+
                        "\n审减-未结清 : "+zbJxOldData.getSjwjq()+
                        "\n应发金额 : "+zbJxOldData.getYfje()+
                        "\n调整(+/-) : "+zbJxOldData.getTz()+
                        "\n实发金额 : "+zbJxOldData.getSfje());
            } else {
                zbZfLog.setOnlyColumnValue(zbzfJxMoney.getXmbh());
                zbZfLog.setNewContent(
                        "绩效季度 : " + zbzfJxMoney.getJxjxjd() +
                                "\n难度系数后A : " + zbzfJxMoney.getNdxsq() +
                                "\n标准金类型B : " + zbzfJxMoney.getBzj() +
                                "\n标准金额B : " + zbzfJxMoney.getBzjq() +
                                "\n多项目系数C : " + zbzfJxMoney.getDxmxsq() +
                                "\n最终绩效D : " + zbzfJxMoney.getZzjxq() +
                                "\n难度系数E : " + zbzfJxMoney.getNdxs() +
                                "\n多项目系数F : " + zbzfJxMoney.getDxmxs() +
                                "\n审减-未结清 : " + zbzfJxMoney.getSjwjq() +
                                "\n应发金额 : " + zbzfJxMoney.getYfje() +
                                "\n调整(+/-) : " + zbzfJxMoney.getTz() +
                                "\n实发金额 : " + zbzfJxMoney.getSfje());
                zbzfService.insertZfLog(zbZfLog);
            }
        }
        return "共勾选"+i+"条数据,新增"+in+"条,修改了"+up+"条";
    }
    /**
     * 修改或新增zf信息
     * @param request
     * @return
     */
    @RequestMapping(value = {"saveOrDeclareZbZf"})
    @ResponseBody
    public String saveOrDeclareZbZf(HttpServletRequest request,@RequestParam(value = "sta" ,required = true) String sta){
        String jsonXtgg= request.getParameter("jsonXtgg");
        List<Zbzf> zbzfList = JSON.parseArray(jsonXtgg, Zbzf.class);
        int up=0,in =0,i;
        for (i=0; i < zbzfList.size(); i++) {
            if (!"".equals(zbzfList.get(i).getProjectnumber())) {
                zbzfService.updateZbZf(zbzfList.get(i),sta);
                up+=1;
            }else{
                zbzfList.get(i).setProjectnumber(zbzfList.get(i).getXmbh());
                zbzfService.insertZbZf(zbzfList.get(i),sta);
                in+=1;
            }
        }
        return "共勾选"+i+"条数据,新增"+in+"条,修改了"+up+"条";
    }


    /**
     * 修改状态值传到前台控件禁用不可再次编辑
     * @param id
     * @return
     */
    @RequestMapping(value = {"updateStatus"})
    @ResponseBody
    public String updateStatus(@RequestParam String id){
        return zbzfService.updateStatus(id)==1?"yes": "no";
    }
    /**
     * 编辑页面删除人
     * @param redirectAttributes
     * @param request
     * @return
     */
    @RequestMapping(value ="deleteZbJxRen")
    @ResponseBody
    public String deleteZbJxRen( RedirectAttributes redirectAttributes, HttpServletRequest request) {
        String renid = request.getParameter("renid");
        try {
            if(renid!="")
                zbzfService.deleteZbJxRen(renid);
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(redirectAttributes, "删除失败");
        }
        return "yes";
    }

    /**
     * 绩效钱统计
     * @param zbjxCount
     * @param model
     * @return
     */
    @RequestMapping(value = "jxmoneycount")
    public String jxmoneycount(ZbjxCount zbjxCount, Model model) {

        List<ZbjxCount> zbJxCountList = zbzfService.jxmoneycount(zbjxCount);

        model.addAttribute("zbJxCountList", zbJxCountList);
        return "modules/zb/zb_zf/jxMoneyCount";
    }


    @RequiresPermissions("zb_zbInfo:ZbInformationForm:view")
    @RequestMapping(value = {"statisticsOfExpensePayment"})
    public String selectCostPaymentCount(Zbzf zbzf, Model model){
        zbzf.setType("1");
        zbzf=zbzfService.ZbUserInfor(zbzf);
        List<ZbjxCount> costPaymentCount=zbzfService.selectCostPaymentCount(zbzf);
        model.addAttribute("cpc", costPaymentCount);
        model.addAttribute("zbzf", zbzf);
        return "modules/zb/zb_zf/statisticsOfExpensePayment";
    }
    @RequiresPermissions("zb_zbInfo:ZbInformationForm:view")
    @RequestMapping(value = {"selectZbfsCount"})
    public String selectZbfsCount(Zbzf zbzf, Model model){
        zbzf.setType("1");
        zbzf=zbzfService.ZbUserInfor(zbzf);
        List<ZbjxCount> selectZbfsCount=zbzfService.selectZbfsCount(zbzf);

        model.addAttribute("cpc", selectZbfsCount);
        model.addAttribute("zbzf", zbzf);
        return "modules/zb/zb_zf/zbfsCount";
    }
    /**
     * 绩效未结清钱统计
     * @param zbjxCount
     * @param model
     * @return
     */
    @RequestMapping(value = "jxWjqMoneyCount")
    public String jxWjqMoneyCount(ZbjxCount zbjxCount, Model model) {
        List<ZbjxCount> zbJxCountList = zbzfService.jxWjqMoneyCount(zbjxCount);
        model.addAttribute("zbJxCountList", zbJxCountList);
        return "modules/zb/zb_zf/jxWjqMoneyCount";
    }



    /**
     * 绩效未结清钱统计
     * @param zbZfLog
     * @param model
     * @return
     */
    @RequestMapping(value = "performanceLog")
    public String performanceLog(ZbZfLog zbZfLog, Model model) {
        List<ZbZfLog> findLogList=  zbzfService.findLogList(zbZfLog);
        for (int i = 0; i < findLogList.size(); i++) {
            findLogList.get(i).setCreateBy(UserUtils.get(findLogList.get(i).getCreateBy().getId()));
        }
        model.addAttribute("list", findLogList);
        return "modules/zb/zb_zf/zfLogList";
    }




}