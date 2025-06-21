package excel_upload_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "modification_history")
public class ModificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long rowEntityId;

    private String operationType; // CREATE, UPDATE, DELETE

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String oldData;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String newData;

    private LocalDateTime timestamp;

    public ModificationHistory() {
    }

    public ModificationHistory(Long id, Long rowEntityId, String operationType, String oldData, String newData, LocalDateTime timestamp) {
        this.id = id;
        this.rowEntityId = rowEntityId;
        this.operationType = operationType;
        this.oldData = oldData;
        this.newData = newData;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRowEntityId() {
        return rowEntityId;
    }

    public void setRowEntityId(Long rowEntityId) {
        this.rowEntityId = rowEntityId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOldData() {
        return oldData;
    }

    public void setOldData(String oldData) {
        this.oldData = oldData;
    }

    public String getNewData() {
        return newData;
    }

    public void setNewData(String newData) {
        this.newData = newData;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ModificationHistory{" +
                "id=" + id +
                ", rowEntityId=" + rowEntityId +
                ", operationType='" + operationType + '\'' +
                ", oldData='" + oldData + '\'' +
                ", newData='" + newData + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
