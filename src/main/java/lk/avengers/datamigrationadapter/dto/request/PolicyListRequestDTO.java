package lk.avengers.datamigrationadapter.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyListRequestDTO {
    private String agent;
    private String unitHead;
    private String salesBranch;
    private String companyBranch;
    private String policyBranch;
    private String introducer;
    private String supervisor;
    private String contract;
    private String currency;
    private String customerName;
    private LocalDate inception;
    private String frequencyMode;
    private String status;
    private BigDecimal annualPremium;
    private LocalDate paidUpTo;
    private BigDecimal unappropriateBalance;
    private String requestUuid;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
