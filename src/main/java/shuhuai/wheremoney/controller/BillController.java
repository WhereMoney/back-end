package shuhuai.wheremoney.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shuhuai.wheremoney.entity.*;
import shuhuai.wheremoney.response.Response;
import shuhuai.wheremoney.response.bill.*;
import shuhuai.wheremoney.service.AssetService;
import shuhuai.wheremoney.service.BillCategoryService;
import shuhuai.wheremoney.service.BillService;
import shuhuai.wheremoney.type.BillType;
import shuhuai.wheremoney.utils.RequestGetter;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bill")
@Api(tags = "账单管理")
@Slf4j
public class BillController extends BaseController {
    @Resource
    private BillService billService;
    @Resource
    private AssetService assetService;
    @Resource
    private BillCategoryService billCategoryService;

    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "token过期"),
            @ApiResponse(code = 422, message = "参数错误"),
            @ApiResponse(code = 500, message = "服务器错误")
    })
    @RequestMapping(value = "/add-bill", method = RequestMethod.POST)
    @ApiOperation(value = "新建账单")
    public Response<Object> addBill(@RequestParam Integer bookId, Integer inAssetId, Integer outAssetId,
                                    Integer billCategoryId, @RequestParam BillType type, @RequestParam BigDecimal amount,
                                    @RequestParam String time, String remark) {
        Timestamp formatDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            formatDate = Timestamp.valueOf(sdf.format(sdf.parse(time)));
        } catch (ParseException error) {
            error.printStackTrace();
        }
        billService.addBill(bookId, inAssetId, outAssetId, billCategoryId, type, amount, formatDate, remark);
        if (inAssetId != null) {
            Asset inAsset = assetService.getAsset(inAssetId);
            int compare = amount.compareTo(new BigDecimal("0.00"));
            if (compare < 0) {
                amount = new BigDecimal("0.00").subtract(amount);
            }// amount 负转正
            inAsset.setBalance(inAsset.getBalance().add(amount)); //资产中更新
            assetService.updateAsset(inAsset);
        }
        if (outAssetId != null) {
            Asset outAsset = assetService.getAsset(outAssetId);
            int compare = amount.compareTo(new BigDecimal("0.00"));
            if (compare > 0) {
                amount = new BigDecimal("0.00").subtract(amount);
            }// amount 正转负
            outAsset.setBalance(outAsset.getBalance().add(amount)); //资产中更新
            assetService.updateAsset(outAsset);
        }
        Response<Object> response = new Response<>(200, "新建账单成功", null);
        log.info("/api/bill/add-bill：" + response.getMessage());
        return response;
    }

    private String[] idToString(BaseBill bill) {
        if (bill instanceof PayBill) {
            String payAsset = assetService.getAsset(((PayBill) bill).getPayAssetId()).getAssetName();
            String billCategory = billCategoryService.getBillCategory(((PayBill) bill).getBillCategoryId()).getBillCategoryName();
            return new String[]{payAsset, billCategory};
        }
        if (bill instanceof IncomeBill) {
            String incomeAsset = assetService.getAsset(((IncomeBill) bill).getIncomeAssetId()).getAssetName();
            String billCategory = billCategoryService.getBillCategory(((IncomeBill) bill).getBillCategoryId()).getBillCategoryName();
            return new String[]{incomeAsset, billCategory};
        }
        if (bill instanceof TransferBill) {
            String inAsset = assetService.getAsset(((TransferBill) bill).getInAssetId()).getAssetName();
            String outAsset = assetService.getAsset(((TransferBill) bill).getOutAssetId()).getAssetName();
            return new String[]{inAsset, outAsset};
        }
        if (bill instanceof RefundBill) {
            String refundAsset = assetService.getAsset(((RefundBill) bill).getRefundAssetId()).getAssetName();
            return new String[]{refundAsset};
        }
        return null;
    }

    private BaseGetBillResponse entityToResponse(BaseBill bill){
        String[] strings=idToString(bill);
        if (bill instanceof PayBill) {
            return new GetPayBillResponse(bill, strings[0], strings[1]);
        }
        if (bill instanceof IncomeBill) {
            return new GetIncomeBillResponse(bill, strings[0], strings[1]);
        }
        if (bill instanceof TransferBill) {
            return new GetTransferBillResponse(bill, strings[0], strings[1]);
        }
        if (bill instanceof RefundBill) {
            return new GetRefundBillResponse(bill, strings[0]);
        }
        return null;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "token过期"),
            @ApiResponse(code = 422, message = "参数错误"),
    })
    @RequestMapping(value = "/bill", method = RequestMethod.GET)
    @ApiOperation(value = "获得账单")
    public Response<BaseGetBillResponse> getBill(@RequestParam Integer id, @RequestParam BillType type) {
        BaseBill bill = billService.getBill(id, type);
        Response<BaseGetBillResponse> response = new Response<>(200, "获得账单成功", entityToResponse(bill));
        log.info(RequestGetter.getRequestUrl() + "：" + response.getMessage());
        return response;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "token过期"),
            @ApiResponse(code = 422, message = "参数错误"),
    })
    @RequestMapping(value = "/all-bill", method = RequestMethod.GET)
    @ApiOperation(value = "获得指定账本的所有账单")
    public Response<GetAllBillResponse> getBillByBook(@RequestParam Integer bookId) {
        List<BaseBill> billList = billService.getBillByBook(bookId);
        List<BaseGetBillResponse> billResponseList = new ArrayList<>();
        for (BaseBill bill : billList) {
            billResponseList.add(entityToResponse(bill));
        }
        Response<GetAllBillResponse> response = new Response<>(200, "获得指定账本的所有账单成功", new GetAllBillResponse(billResponseList));
        log.info(RequestGetter.getRequestUrl() + "：" + response.getMessage());
        return response;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "token过期"),
            @ApiResponse(code = 422, message = "参数错误"),
    })
    @RequestMapping(value = "/all-bill-time", method = RequestMethod.GET)
    @ApiOperation(value = "获得指定账本的所有账单时间")
    public Response<GetAllBillResponse> getBillByBookTIme(@RequestParam Integer bookId, @RequestParam Timestamp startTime, @RequestParam Timestamp endTime) {
        List<BaseBill> billList = billService.getBillByBookTime(bookId, startTime, endTime);
        List<BaseGetBillResponse> billResponseList = new ArrayList<>();
        for (BaseBill bill : billList) {
            billResponseList.add(entityToResponse(bill));
        }
        Response<GetAllBillResponse> response = new Response<>(200, "获得指定账本的所有账单时间成功", new GetAllBillResponse(billResponseList));
        log.info(RequestGetter.getRequestUrl() + "：" + response.getMessage());
        return response;
    }

    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "token过期"),
            @ApiResponse(code = 422, message = "参数错误"),
    })
    @RequestMapping(value = "/category-statistic-time", method = RequestMethod.GET)
    @ApiOperation(value = "分类统计指定账本的指定时间段的账单")
    public Response<CategoryStatisticResponse> getCategoryStatisticTime(@RequestParam Integer bookId, @RequestParam Timestamp startTime, @RequestParam Timestamp endTime) {
        List<Map<String, Object>> payStatistic = billService.getCategoryPayStatisticTime(bookId, startTime, endTime);
        List<Map<String, Object>> incomeStatistic = billService.getCategoryIncomeStatisticTime(bookId, startTime, endTime);
        Response<CategoryStatisticResponse> response = new Response<>(200, "分类统计指定账本的指定时间段的账单成功",
                new CategoryStatisticResponse(payStatistic, incomeStatistic));
        log.info(RequestGetter.getRequestUrl() + "：" + response.getMessage());
        return response;
    }
}