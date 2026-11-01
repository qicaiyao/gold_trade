package com.bank.gold.service;

import com.bank.gold.common.GoldConstants;
import com.bank.gold.entity.GoldTranLog;
import com.bank.gold.repository.GoldTranLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 交易流水的写入动作。
 * <p>独立事务（REQUIRES_NEW）：主交易回滚时，流水记录仍需保留，否则幂等与审计失效。</p>
 */
@Service
public class TranLogWriter {

    private final GoldTranLogRepository repository;

    public TranLogWriter(GoldTranLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 插入流水，依靠 req_serial_no 唯一索引实现幂等占位。
     *
     * @throws org.springframework.dao.DataIntegrityViolationException 流水号重复
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insertNew(String reqSerialNo, String tranCode) {
        LocalDateTime now = LocalDateTime.now();
        GoldTranLog log = new GoldTranLog();
        log.setReqSerialNo(reqSerialNo);
        log.setTranCode(tranCode);
        log.setStatus(GoldConstants.TRAN_PROCESSING);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        repository.saveAndFlush(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long id) {
        repository.findById(id).ifPresent(log -> {
            log.setStatus(GoldConstants.TRAN_PROCESSING);
            log.setUpdateTime(LocalDateTime.now());
            repository.save(log);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishSuccess(String reqSerialNo, String respXml, long costMs) {
        repository.findByReqSerialNo(reqSerialNo).ifPresent(log -> {
            log.setStatus(GoldConstants.TRAN_SUCCESS);
            log.setRespCode("000000");
            log.setRespMsg("交易成功");
            log.setRespXml(respXml);
            log.setCostMs(costMs);
            log.setUpdateTime(LocalDateTime.now());
            repository.save(log);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishFail(String reqSerialNo, String respCode, String respMsg, String respXml, long costMs) {
        repository.findByReqSerialNo(reqSerialNo).ifPresent(log -> {
            log.setStatus(GoldConstants.TRAN_FAIL);
            log.setRespCode(respCode);
            log.setRespMsg(truncate(respMsg, 250));
            log.setRespXml(respXml);
            log.setCostMs(costMs);
            log.setUpdateTime(LocalDateTime.now());
            repository.save(log);
        });
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
