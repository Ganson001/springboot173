
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 疫苗信息
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/yimiao")
public class YimiaoController {
    private static final Logger logger = LoggerFactory.getLogger(YimiaoController.class);

    @Autowired
    private YimiaoService yimiaoService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service
    @Autowired
    private YiyuanService yiyuanService;

    @Autowired
    private YonghuService yonghuService;
    @Autowired
    private YishengService yishengService;


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        else if("医生".equals(role))
            params.put("yishengId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = yimiaoService.queryPage(params);

        //字典表数据转换
        List<YimiaoView> list =(List<YimiaoView>)page.getList();
        for(YimiaoView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        YimiaoEntity yimiao = yimiaoService.selectById(id);
        if(yimiao !=null){
            //entity转view
            YimiaoView view = new YimiaoView();
            BeanUtils.copyProperties( yimiao , view );//把实体数据重构到view中

                //级联表
                YiyuanEntity yiyuan = yiyuanService.selectById(yimiao.getYiyuanId());
                if(yiyuan != null){
                    BeanUtils.copyProperties( yiyuan , view ,new String[]{ "id", "createTime", "insertTime", "updateTime"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYiyuanId(yiyuan.getId());
                }
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody YimiaoEntity yimiao, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,yimiao:{}",this.getClass().getName(),yimiao.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<YimiaoEntity> queryWrapper = new EntityWrapper<YimiaoEntity>()
            .eq("yimiao_name", yimiao.getYimiaoName())
            .eq("yimiao_types", yimiao.getYimiaoTypes())
            .eq("yiyuan_id", yimiao.getYiyuanId())
            .eq("yimiao_clicknum", yimiao.getYimiaoClicknum())
            .eq("insert_time", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YimiaoEntity yimiaoEntity = yimiaoService.selectOne(queryWrapper);
        if(yimiaoEntity==null){
            yimiao.setYimiaoClicknum(1);
            yimiao.setInsertTime(new Date());
            yimiao.setCreateTime(new Date());
            yimiaoService.insert(yimiao);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody YimiaoEntity yimiao, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,yimiao:{}",this.getClass().getName(),yimiao.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        //根据字段查询是否有相同数据
        Wrapper<YimiaoEntity> queryWrapper = new EntityWrapper<YimiaoEntity>()
            .notIn("id",yimiao.getId())
            .andNew()
            .eq("yimiao_name", yimiao.getYimiaoName())
            .eq("yimiao_types", yimiao.getYimiaoTypes())
            .eq("yiyuan_id", yimiao.getYiyuanId())
            .eq("yimiao_clicknum", yimiao.getYimiaoClicknum())
            .eq("insert_time", new SimpleDateFormat("yyyy-MM-dd").format(yimiao.getInsertTime()))
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YimiaoEntity yimiaoEntity = yimiaoService.selectOne(queryWrapper);
        if("".equals(yimiao.getYimiaoPhoto()) || "null".equals(yimiao.getYimiaoPhoto())){
                yimiao.setYimiaoPhoto(null);
        }
        if(yimiaoEntity==null){
            yimiaoService.updateById(yimiao);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        yimiaoService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        try {
            List<YimiaoEntity> yimiaoList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            YimiaoEntity yimiaoEntity = new YimiaoEntity();
//                            yimiaoEntity.setYimiaoName(data.get(0));                    //疫苗标题 要改的
//                            yimiaoEntity.setYimiaoTypes(Integer.valueOf(data.get(0)));   //疫苗类型 要改的
//                            yimiaoEntity.setYimiaoPhoto("");//照片
//                            yimiaoEntity.setYiyuanId(Integer.valueOf(data.get(0)));   //医院信息 要改的
//                            yimiaoEntity.setYimiaoClicknum(Integer.valueOf(data.get(0)));   //点击次数 要改的
//                            yimiaoEntity.setInsertTime(date);//时间
//                            yimiaoEntity.setYimiaoContent("");//照片
//                            yimiaoEntity.setCreateTime(date);//时间
                            yimiaoList.add(yimiaoEntity);


                            //把要查询是否重复的字段放入map中
                        }

                        //查询是否重复
                        yimiaoService.insertBatch(yimiaoList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }





    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        // 没有指定排序字段就默认id倒序
        if(StringUtil.isEmpty(String.valueOf(params.get("orderBy")))){
            params.put("orderBy","id");
        }
        PageUtils page = yimiaoService.queryPage(params);

        //字典表数据转换
        List<YimiaoView> list =(List<YimiaoView>)page.getList();
        for(YimiaoView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        YimiaoEntity yimiao = yimiaoService.selectById(id);
            if(yimiao !=null){

                //点击数量加1
                yimiao.setYimiaoClicknum(yimiao.getYimiaoClicknum()+1);
                yimiaoService.updateById(yimiao);

                //entity转view
                YimiaoView view = new YimiaoView();
                BeanUtils.copyProperties( yimiao , view );//把实体数据重构到view中

                //级联表
                    YiyuanEntity yiyuan = yiyuanService.selectById(yimiao.getYiyuanId());
                if(yiyuan != null){
                    BeanUtils.copyProperties( yiyuan , view ,new String[]{ "id", "createDate"});//把级联的数据添加到view中,并排除id和创建时间字段
                    view.setYiyuanId(yiyuan.getId());
                }
                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody YimiaoEntity yimiao, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,yimiao:{}",this.getClass().getName(),yimiao.toString());
        Wrapper<YimiaoEntity> queryWrapper = new EntityWrapper<YimiaoEntity>()
            .eq("yimiao_name", yimiao.getYimiaoName())
            .eq("yimiao_types", yimiao.getYimiaoTypes())
            .eq("yiyuan_id", yimiao.getYiyuanId())
            .eq("yimiao_clicknum", yimiao.getYimiaoClicknum())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        YimiaoEntity yimiaoEntity = yimiaoService.selectOne(queryWrapper);
        if(yimiaoEntity==null){
            yimiao.setInsertTime(new Date());
            yimiao.setCreateTime(new Date());
        yimiaoService.insert(yimiao);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }


}
