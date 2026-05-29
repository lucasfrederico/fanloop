{{- define "fanloop.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "fanloop.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "fanloop.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "fanloop.labels" -}}
app.kubernetes.io/name: {{ include "fanloop.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "fanloop.selectorLabels" -}}
app.kubernetes.io/name: {{ include "fanloop.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "fanloop.redisHost" -}}
{{ .Release.Name }}-redis-master
{{- end -}}
