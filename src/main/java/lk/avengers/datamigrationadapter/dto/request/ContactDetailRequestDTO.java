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
public class ContactDetailRequestDTO {
    private String product;
    private String policyNo;
    private Integer pin;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer currentAge;
    private String nicNumber;
    private String occupation;
    private String address;
    private String city;
    private String mobile;
    private String otherTelephoneNumber;
    private String emailAddress;
    private String policyStatus;
    private Integer nbrOfCustomers;
    private String nationality;
    private String agentCode;
    private String salesBranch;
    private BigDecimal totalPremiumsPaid;
    private BigDecimal outstanding;
    private BigDecimal modalPremiumWithoutHandlingFee;
    private LocalDate policyInceptionDate;
    private LocalDate policyIssueDate;
    private String frequency;
    private LocalDate nextPremiumDueDate;
    private LocalDate lastPremiumPaymentDueDate;
    private LocalDate policyExpiryDate;
    private Integer term;
    private LocalDate lapsedDate;
    private LocalDate expiryDate;
    private String languagePreference;

    public <E> E mapData(Class<E> receiverClass) {
        E receiver = BeanUtils.instantiateClass(receiverClass);
        BeanUtils.copyProperties(this, receiver);
        return receiver;
    }
}
