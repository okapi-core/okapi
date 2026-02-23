{{- define "okapi-ingester.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "okapi-ingester.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "okapi-ingester.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "okapi-ingester.labels" -}}
app.kubernetes.io/name: {{ include "okapi-ingester.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "okapi-ingester.selectorLabels" -}}
app.kubernetes.io/name: {{ include "okapi-ingester.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
