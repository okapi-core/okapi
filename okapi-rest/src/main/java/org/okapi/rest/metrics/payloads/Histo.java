package org.okapi.rest.metrics.payloads;

import java.util.List;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Histo {
  List<HistoPoint> histoPoints;
}
