package lk.avengers.datamigrationadapter.entity.postgresql.reportdb;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_pin")
public class BankPinEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer pin;
    private String bank;
}
