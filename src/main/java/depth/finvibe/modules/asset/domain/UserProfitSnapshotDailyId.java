package depth.finvibe.modules.asset.domain;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProfitSnapshotDailyId implements Serializable {
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;
}
