package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "advisor")
public class AdvisorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch")
    private String branch;

    @Column(name = "agent")
    private String agent;

    @Column(name = "designation")
    private String designation;

    @Column(name = "id_nic", length = 20)
    private String idNic;

    @Column(name = "unit_head")
    private String unitHead;

    @Column(name = "reporting_to")
    private String reportingTo;

    @Column(name = "in_date")
    private LocalDate inDate;

    @Column(name = "appointment_date")
    private LocalDate appointmentDate;

    @Column(name = "agent_status")
    private String agentStatus;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "reason_for_termination", columnDefinition = "TEXT")
    private String reasonForTermination;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "mobile_no", length = 20)
    private String mobileNo;

    @Column(name = "ibsl_no", length = 30)
    private String ibslNo;
}
