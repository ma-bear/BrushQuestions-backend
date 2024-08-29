package com.xq.mianshiya.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xq.mianshiya.annotation.AuthCheck;
import com.xq.mianshiya.common.BaseResponse;
import com.xq.mianshiya.common.DeleteRequest;
import com.xq.mianshiya.common.ErrorCode;
import com.xq.mianshiya.common.ResultUtils;
import com.xq.mianshiya.constant.UserConstant;
import com.xq.mianshiya.exception.BusinessException;
import com.xq.mianshiya.exception.ThrowUtils;
import com.xq.mianshiya.model.dto.question.QuestionQueryRequest;
import com.xq.mianshiya.model.dto.questionBank.QuestionBankAddRequest;
import com.xq.mianshiya.model.dto.questionBank.QuestionBankEditRequest;
import com.xq.mianshiya.model.dto.questionBank.QuestionBankQueryRequest;
import com.xq.mianshiya.model.dto.questionBank.QuestionBankUpdateRequest;
import com.xq.mianshiya.model.entity.Question;
import com.xq.mianshiya.model.entity.QuestionBank;
import com.xq.mianshiya.model.entity.User;
import com.xq.mianshiya.model.vo.QuestionBankVO;
import com.xq.mianshiya.service.QuestionBankService;
import com.xq.mianshiya.service.QuestionService;
import com.xq.mianshiya.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题库接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/questionbank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;

    // region 增删改查

    /**
     * 创建题库
     *
     * @param questionbankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionbankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionbankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionbank = new QuestionBank();
        BeanUtils.copyProperties(questionbankAddRequest, questionbank);
        // 数据校验
        questionBankService.validQuestionBank(questionbank, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionbank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionbank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionbank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionbankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionbankUpdateRequest) {
        if (questionbankUpdateRequest == null || questionbankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionbank = new QuestionBank();
        BeanUtils.copyProperties(questionbankUpdateRequest, questionbank);
        // 数据校验
        questionBankService.validQuestionBank(questionbank, false);
        // 判断是否存在
        long id = questionbankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateById(questionbank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        QuestionBank questionbank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionbank == null, ErrorCode.NOT_FOUND_ERROR);
        // 查询题库封装类
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionbank, request);
        // 是否关联查询题目列表
        boolean needQueryQuestionList = questionBankQueryRequest.isNeedQueryQuestionList();
        if (needQueryQuestionList){
            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }
        // 获取封装类
        return ResultUtils.success(questionBankVO);
    }

    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionbankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionbankQueryRequest) {
        long current = questionbankQueryRequest.getCurrent();
        long size = questionbankQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank> questionbankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionbankQueryRequest));
        return ResultUtils.success(questionbankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     *
     * @param questionbankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionbankQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionbankQueryRequest.getCurrent();
        long size = questionbankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionbankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionbankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionbankPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionbankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionbankQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionbankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionbankQueryRequest.setUserId(loginUser.getId());
        long current = questionbankQueryRequest.getCurrent();
        long size = questionbankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionbankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionbankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionbankPage, request));
    }

    /**
     * 编辑题库（给用户使用）
     *
     * @param questionbankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionbankEditRequest, HttpServletRequest request) {
        if (questionbankEditRequest == null || questionbankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionbank = new QuestionBank();
        BeanUtils.copyProperties(questionbankEditRequest, questionbank);
        // 数据校验
        questionBankService.validQuestionBank(questionbank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionbankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateById(questionbank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
