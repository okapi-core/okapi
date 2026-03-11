package org.okapi.oscar.session;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.rest.session.SESSION_STATE;

@Entity
@Table(name = "oscar_session_meta")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SessionMetaEntity {

  @Id
  @Column(name = "session_id", nullable = false)
  String sessionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  SESSION_STATE state;

  @Column(name = "start_time", nullable = false)
  long startTime;

  @Column(name = "last_recorded_ping", nullable = false)
  long lastRecordedPing;

  @OneToOne(optional = true)
  @JoinColumn(name = "ongoing_stream_id")
  StreamMetaEntity ongoingStream;
}
