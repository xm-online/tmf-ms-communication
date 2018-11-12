package com.icthh.xm.tmf.ms.communication.service.tenant;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class TenantService {

    public static final String TENANT_XM = "XM";
    private final TenantContextHolder tenantContextHolder;
    private final TenantListRepository tenantListRepository;


    /**
     * Create tenant.
     *
     * @param tenant tenant key
     */
    @SneakyThrows
    public void createTenant(String tenant) {
        StopWatch stopWatch = StopWatch.createStarted();

        String tenantKey = formatTenantKey(tenant);

        log.info("START - SETUP:CreateTenant: tenantKey: {}", tenantKey);

        checkOnlySuperTenantOperation();

        try {
            tenantListRepository.addTenant(tenantKey);
            log.info("STOP  - SETUP:CreateTenant: tenantKey: {}, result: OK, time = {} ms",
                     tenantKey,
                     stopWatch.getTime());
        } catch (Exception e) {
            log.error("STOP  - SETUP:CreateTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                      tenantKey,
                      e.getMessage(),
                      stopWatch.getTime());
            throw e;
        }
    }

    /**
     * Delete tenant.
     *
     * @param tenant tenant key
     */
    public void deleteTenant(String tenant) {
        StopWatch stopWatch = StopWatch.createStarted();

        String tenantKey = formatTenantKey(tenant);

        log.info("START - SETUP:DeleteTenant: tenantKey: {}", tenantKey);

        try {
            tenantListRepository.deleteTenant(tenantKey);

            log.info("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: OK, time = {} ms",
                     tenantKey,
                     stopWatch.getTime());
        } catch (Exception e) {
            log.error("STOP  - SETUP:DeleteTenant: tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                      tenantKey,
                      e.getMessage(),
                      stopWatch.getTime());
            throw e;
        }
    }

    public void manageTenant(String tenant, String state) {
        StopWatch stopWatch = StopWatch.createStarted();

        String tenantKey = formatTenantKey(tenant);

        log.info("START - SETUP:ManageTenant: tenantKey: {}, state: {}", tenantKey, state);

        try {
            tenantListRepository.updateTenant(tenantKey, state.toUpperCase());

            log.info("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: OK, time = {} ms",
                     tenantKey,
                     state,
                     stopWatch.getTime());
        } catch (Exception e) {
            log.error("STOP  - SETUP:ManageTenant: tenantKey: {}, state: {}, result: FAIL, error: {}, time = {} ms",
                      tenantKey,
                      state,
                      e.getMessage(),
                      stopWatch.getTime());
            throw e;
        }
    }

    private void checkOnlySuperTenantOperation() {
        if (!TENANT_XM.equals(TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))) {
            throw new BusinessException("Only 'XM' tenant is allowed to create new tenants");
        }
    }

    private String formatTenantKey(String tenant) {
        return StringUtils.upperCase(tenant);
    }

}
