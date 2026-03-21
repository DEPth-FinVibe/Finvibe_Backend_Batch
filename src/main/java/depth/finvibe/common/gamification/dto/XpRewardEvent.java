package depth.finvibe.common.gamification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
@Builder
public class XpRewardEvent {
    private String userId;
    private String reason;
    private Long xpAmount;
}
