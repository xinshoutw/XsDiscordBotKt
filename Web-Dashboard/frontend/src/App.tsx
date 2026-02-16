import { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  CloudOff,
  FileCode2,
  LoaderCircle,
  Puzzle,
  RefreshCcw,
  Save
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

type LogType = "command" | "button" | "modal";

type ConsoleTarget = {
  guildId: number;
  channelId: number;
  logTypes: LogType[];
  format: string;
};

type CoreConfig = {
  general: {
    tokenMasked: string;
  };
  builtins: {
    statusMessages: string[];
    consoleTargets: ConsoleTarget[];
  };
};

type PluginConfig = {
  name: string;
  enabled: boolean;
  category: string;
  description: string;
  dependencies: string[];
  intents: string[];
  author?: string | null;
  version?: string | null;
  requireIntents: string[];
  requireCacheFlags: string[];
  requireMemberCachePolicies: string[];
  dependPlugins: string[];
  softDependPlugins: string[];
  loaded: boolean;
  canToggle: boolean;
  configPath?: string | null;
  hasWebEditor: boolean;
};

type PluginYaml = {
  name: string;
  yaml: string;
  path: string;
  loaded: boolean;
  hasEnabledFlag: boolean;
};

type SaveResponse = {
  ok: boolean;
  message: string;
  updatedAt: number;
};

type Health = {
  status: string;
  serverTime: number;
  mode: string;
  recommendedBaseUrl: string;
};

type ActivityType =
  | "PLAYING"
  | "STREAMING"
  | "LISTENING"
  | "WATCHING"
  | "COMPETING"
  | "CUSTOM_STATUS";

type StatusEntry = {
  type: ActivityType;
  content: string;
  delayMs: number;
};

const STATUS_TYPES: ActivityType[] = [
  "PLAYING",
  "STREAMING",
  "LISTENING",
  "WATCHING",
  "COMPETING",
  "CUSTOM_STATUS"
];

const DEFAULT_STATUS_ENTRY: StatusEntry = {
  type: "COMPETING",
  content: "Developing...",
  delayMs: 5000
};

function normalizeYaml(input: string): string {
  return input.replace(/\r\n/g, "\n").trim();
}

function parseStatusLine(line: string): StatusEntry {
  const first = line.indexOf(";");
  const last = line.lastIndexOf(";");

  if (first <= 0 || last <= first) {
    return DEFAULT_STATUS_ENTRY;
  }

  const rawType = line.slice(0, first).trim();
  const rawContent = line.slice(first + 1, last);
  const rawDelay = Number(line.slice(last + 1).trim());

  const type = STATUS_TYPES.includes(rawType as ActivityType)
    ? (rawType as ActivityType)
    : DEFAULT_STATUS_ENTRY.type;

  return {
    type,
    content: rawContent,
    delayMs: Number.isFinite(rawDelay) && rawDelay > 0 ? Math.floor(rawDelay) : DEFAULT_STATUS_ENTRY.delayMs
  };
}

function formatStatusLine(entry: StatusEntry): string {
  const delay = Number.isFinite(entry.delayMs) && entry.delayMs > 0 ? Math.floor(entry.delayMs) : DEFAULT_STATUS_ENTRY.delayMs;
  return `${entry.type};${entry.content};${delay}`;
}

async function readErrorMessage(response: Response): Promise<string> {
  const text = await response.text();
  if (!text) {
    return `Request failed (${response.status})`;
  }

  try {
    const parsed = JSON.parse(text) as Partial<SaveResponse>;
    if (typeof parsed.message === "string" && parsed.message.trim().length > 0) {
      return parsed.message;
    }
  } catch {
    // Use raw text.
  }

  return text;
}

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json"
    }
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message);
  }

  return (await response.json()) as T;
}

async function putJson<TRequest, TResponse>(url: string, payload: TRequest): Promise<TResponse> {
  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message);
  }

  return (await response.json()) as TResponse;
}

function renderTagList(title: string, items: string[]) {
  return (
    <div>
      <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">{title}</p>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {(items.length > 0 ? items : ["None"]).map((item) => (
          <Badge key={`${title}-${item}`} variant="outline">
            {item}
          </Badge>
        ))}
      </div>
    </div>
  );
}

function App() {
  const [health, setHealth] = useState<Health | null>(null);
  const [core, setCore] = useState<CoreConfig | null>(null);
  const [coreDraft, setCoreDraft] = useState<CoreConfig | null>(null);
  const [plugins, setPlugins] = useState<PluginConfig[]>([]);
  const [selectedPluginName, setSelectedPluginName] = useState<string | null>(null);

  const [pluginYaml, setPluginYaml] = useState<PluginYaml | null>(null);
  const [pluginYamlDraft, setPluginYamlDraft] = useState<string>("");

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [savingCore, setSavingCore] = useState(false);
  const [savingYaml, setSavingYaml] = useState(false);
  const [loadingYaml, setLoadingYaml] = useState(false);
  const [toggleBusy, setToggleBusy] = useState<Record<string, boolean>>({});
  const [message, setMessage] = useState<string>("");

  useEffect(() => {
    void refreshAll();
  }, []);

  const selectedPlugin = useMemo(
    () => plugins.find((plugin) => plugin.name === selectedPluginName) ?? plugins[0] ?? null,
    [plugins, selectedPluginName]
  );

  const togglePlugins = useMemo(
    () => plugins.filter((plugin) => plugin.canToggle),
    [plugins]
  );

  const fixedPlugins = useMemo(
    () => plugins.filter((plugin) => !plugin.canToggle),
    [plugins]
  );

  useEffect(() => {
    if (selectedPlugin?.hasWebEditor) {
      void loadPluginYaml(selectedPlugin.name);
    } else {
      setPluginYaml(null);
      setPluginYamlDraft("");
    }
  }, [selectedPlugin?.name, selectedPlugin?.hasWebEditor]);

  const dirtyCore = useMemo(
    () => JSON.stringify(coreDraft) !== JSON.stringify(core),
    [core, coreDraft]
  );

  const dirtyPluginYaml = useMemo(() => {
    if (!pluginYaml) return false;
    return normalizeYaml(pluginYamlDraft) !== normalizeYaml(pluginYaml.yaml);
  }, [pluginYaml, pluginYamlDraft]);

  async function refreshAll() {
    setLoading(true);
    setMessage("");
    try {
      const [healthData, coreData, pluginData] = await Promise.all([
        fetchJson<Health>("/api/v1/health"),
        fetchJson<CoreConfig>("/api/v1/config/core"),
        fetchJson<PluginConfig[]>("/api/v1/config/plugins")
      ]);

      setHealth(healthData);
      setCore(coreData);
      setCoreDraft(coreData);
      setPlugins(pluginData);
      setSelectedPluginName((current) => current ?? pluginData[0]?.name ?? null);
    } catch (error) {
      const readable = error instanceof Error ? error.message : "Unknown fetch error";
      setMessage(`Dashboard initialization failed: ${readable}`);
    } finally {
      setLoading(false);
    }
  }

  async function refreshPlugins() {
    setRefreshing(true);
    try {
      const pluginData = await fetchJson<PluginConfig[]>("/api/v1/config/plugins");
      setPlugins(pluginData);
      setSelectedPluginName((current) => {
        if (current && pluginData.some((plugin) => plugin.name === current)) {
          return current;
        }
        return pluginData[0]?.name ?? null;
      });
    } finally {
      setRefreshing(false);
    }
  }

  async function loadPluginYaml(pluginName: string) {
    setLoadingYaml(true);
    try {
      const payload = await fetchJson<PluginYaml>(
        `/api/v1/config/plugins/${encodeURIComponent(pluginName)}/yaml`
      );
      setPluginYaml(payload);
      setPluginYamlDraft(payload.yaml);
    } catch (error) {
      const readable = error instanceof Error ? error.message : "Unknown plugin config fetch error";
      setPluginYaml(null);
      setPluginYamlDraft("");
      setMessage(`Cannot load plugin config for ${pluginName}: ${readable}`);
    } finally {
      setLoadingYaml(false);
    }
  }

  function updateStatusEntry(index: number, patch: Partial<StatusEntry>) {
    setCoreDraft((current) => {
      if (!current) return current;
      const next = [...current.builtins.statusMessages];
      const previous = parseStatusLine(next[index] ?? formatStatusLine(DEFAULT_STATUS_ENTRY));
      next[index] = formatStatusLine({ ...previous, ...patch });

      return {
        ...current,
        builtins: {
          ...current.builtins,
          statusMessages: next
        }
      };
    });
  }

  function addStatusMessage() {
    setCoreDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        builtins: {
          ...current.builtins,
          statusMessages: [...current.builtins.statusMessages, formatStatusLine(DEFAULT_STATUS_ENTRY)]
        }
      };
    });
  }

  function removeStatusMessage(index: number) {
    setCoreDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        builtins: {
          ...current.builtins,
          statusMessages: current.builtins.statusMessages.filter((_, i) => i !== index)
        }
      };
    });
  }

  function updateConsoleTarget(
    index: number,
    patch: Partial<Omit<ConsoleTarget, "logTypes">> & { logTypes?: LogType[] }
  ) {
    setCoreDraft((current) => {
      if (!current) return current;
      const nextTargets = [...current.builtins.consoleTargets];
      nextTargets[index] = {
        ...nextTargets[index],
        ...patch
      };

      return {
        ...current,
        builtins: {
          ...current.builtins,
          consoleTargets: nextTargets
        }
      };
    });
  }

  function addConsoleTarget() {
    setCoreDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        builtins: {
          ...current.builtins,
          consoleTargets: [
            ...current.builtins.consoleTargets,
            {
              guildId: 0,
              channelId: 0,
              logTypes: ["command"],
              format: "[%cl_type%] %user_name% `%cl_interaction_string%`"
            }
          ]
        }
      };
    });
  }

  function removeConsoleTarget(index: number) {
    setCoreDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        builtins: {
          ...current.builtins,
          consoleTargets: current.builtins.consoleTargets.filter((_, i) => i !== index)
        }
      };
    });
  }

  async function saveCore() {
    if (!coreDraft) return;
    setSavingCore(true);
    try {
      const normalized: CoreConfig = {
        ...coreDraft,
        builtins: {
          ...coreDraft.builtins,
          statusMessages: coreDraft.builtins.statusMessages.map((line) => formatStatusLine(parseStatusLine(line)))
        }
      };

      const response = await putJson<CoreConfig, SaveResponse>("/api/v1/config/core", normalized);
      setCore(normalized);
      setCoreDraft(normalized);
      setMessage(response.message);
    } catch (error) {
      const readable = error instanceof Error ? error.message : "Unknown core save error";
      setMessage(`Save core failed: ${readable}`);
    } finally {
      setSavingCore(false);
    }
  }

  async function togglePluginImmediate(plugin: PluginConfig, nextEnabled: boolean) {
    if (!plugin.canToggle) {
      setMessage(`Plugin ${plugin.name} cannot be toggled automatically.`);
      return;
    }

    const previous = plugin.enabled;
    setToggleBusy((current) => ({ ...current, [plugin.name]: true }));
    setPlugins((current) =>
      current.map((item) => (item.name === plugin.name ? { ...item, enabled: nextEnabled } : item))
    );

    try {
      const response = await putJson<{ enabled: boolean }, SaveResponse>(
        `/api/v1/config/plugins/${encodeURIComponent(plugin.name)}`,
        { enabled: nextEnabled }
      );
      setMessage(response.message);
      await refreshPlugins();
      if (selectedPlugin?.name === plugin.name && selectedPlugin.hasWebEditor) {
        await loadPluginYaml(plugin.name);
      }
    } catch (error) {
      setPlugins((current) =>
        current.map((item) => (item.name === plugin.name ? { ...item, enabled: previous } : item))
      );
      const readable = error instanceof Error ? error.message : "Unknown plugin toggle error";
      setMessage(`Toggle plugin ${plugin.name} failed: ${readable}`);
    } finally {
      setToggleBusy((current) => ({ ...current, [plugin.name]: false }));
    }
  }

  async function savePluginYaml() {
    if (!selectedPlugin || !pluginYaml) return;
    setSavingYaml(true);
    try {
      const response = await putJson<{ yaml: string }, SaveResponse>(
        `/api/v1/config/plugins/${encodeURIComponent(selectedPlugin.name)}/yaml`,
        { yaml: pluginYamlDraft }
      );
      setPluginYaml((current) => (current ? { ...current, yaml: pluginYamlDraft } : current));
      setMessage(response.message);
      await refreshPlugins();
    } catch (error) {
      const readable = error instanceof Error ? error.message : "Unknown plugin save error";
      setMessage(`Save plugin YAML failed: ${readable}`);
    } finally {
      setSavingYaml(false);
    }
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="pointer-events-none fixed inset-0 -z-10 overflow-hidden">
        <div className="absolute left-[-120px] top-[-80px] size-[420px] rounded-full bg-primary/20 blur-[80px]" />
        <div className="absolute right-[-100px] top-[24%] size-[360px] rounded-full bg-warning/15 blur-[100px]" />
        <div className="absolute bottom-[-120px] left-[26%] size-[420px] rounded-full bg-success/15 blur-[110px]" />
      </div>

      <div className="container py-6 lg:py-10">
        <header className="rounded-xl border border-border/80 bg-card/90 p-5 shadow-soft backdrop-blur">
          <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="font-heading text-2xl font-semibold md:text-3xl">XsDiscordBot 控制面板</h1>
              <p className="mt-1 text-sm text-muted-foreground">插件開關即時套用，設定與 YAML 以按鈕儲存。</p>
            </div>

            <div className="flex items-center gap-2">
              <Badge variant={health?.status === "ok" ? "success" : "warning"}>
                <CheckCircle2 className="mr-1.5 size-3.5" />
                {health?.status === "ok" ? "Connected" : "Offline"}
              </Badge>
              <Button
                variant="outline"
                onClick={() => void refreshAll()}
                disabled={loading || refreshing || savingCore || savingYaml}
              >
                <RefreshCcw className={cn("size-4", (loading || refreshing) && "animate-spin")} />
                重新整理
              </Button>
            </div>
          </div>

          {message && (
            <div className="mt-4 rounded-lg border border-border bg-background/60 px-3 py-2 text-sm text-muted-foreground">
              {message}
            </div>
          )}
        </header>

        <main className="mt-6">
          <Tabs defaultValue="core" className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="core">Core Settings</TabsTrigger>
              <TabsTrigger value="plugins">Plugin Controls</TabsTrigger>
            </TabsList>

            <TabsContent value="core" className="space-y-4">
              <Card className="bg-card/95 backdrop-blur">
                <CardHeader>
                  <CardTitle>Status Changer</CardTitle>
                  <CardDescription>使用 Type / Message / Delay 編輯，不再手動打整串分號格式。</CardDescription>
                </CardHeader>
                <CardContent className="space-y-3">
                  {(coreDraft?.builtins.statusMessages ?? []).map((line, index) => {
                    const entry = parseStatusLine(line);
                    return (
                      <div key={index} className="grid gap-2 md:grid-cols-[170px_1fr_140px_auto]">
                        <select
                          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                          value={entry.type}
                          onChange={(event) => updateStatusEntry(index, { type: event.target.value as ActivityType })}
                        >
                          {STATUS_TYPES.map((type) => (
                            <option key={type} value={type}>
                              {type}
                            </option>
                          ))}
                        </select>

                        <Input
                          value={entry.content}
                          onChange={(event) => updateStatusEntry(index, { content: event.target.value })}
                          placeholder="Status text"
                        />

                        <Input
                          type="number"
                          min={1000}
                          step={1000}
                          value={entry.delayMs}
                          onChange={(event) => updateStatusEntry(index, { delayMs: Number(event.target.value) || 1000 })}
                        />

                        <Button
                          variant="outline"
                          onClick={() => removeStatusMessage(index)}
                          disabled={(coreDraft?.builtins.statusMessages.length ?? 0) <= 1}
                        >
                          Remove
                        </Button>
                      </div>
                    );
                  })}

                  <Button variant="outline" onClick={addStatusMessage}>
                    Add Status
                  </Button>
                </CardContent>
              </Card>

              <Card className="bg-card/95 backdrop-blur">
                <CardHeader>
                  <CardTitle>Console Logger Targets</CardTitle>
                  <CardDescription>多欄位設定，按下儲存後套用。</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {(coreDraft?.builtins.consoleTargets ?? []).map((target, index) => (
                    <div key={index} className="rounded-lg border p-4">
                      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                        <div className="space-y-1">
                          <label className="text-xs text-muted-foreground">Guild ID</label>
                          <Input
                            value={target.guildId}
                            onChange={(event) =>
                              updateConsoleTarget(index, {
                                guildId: Number(event.target.value) || 0
                              })
                            }
                          />
                        </div>
                        <div className="space-y-1">
                          <label className="text-xs text-muted-foreground">Channel ID</label>
                          <Input
                            value={target.channelId}
                            onChange={(event) =>
                              updateConsoleTarget(index, {
                                channelId: Number(event.target.value) || 0
                              })
                            }
                          />
                        </div>
                        <div className="space-y-1 md:col-span-2">
                          <label className="text-xs text-muted-foreground">Format</label>
                          <Input
                            value={target.format}
                            onChange={(event) =>
                              updateConsoleTarget(index, { format: event.target.value })
                            }
                          />
                        </div>
                      </div>

                      <div className="mt-3 flex flex-wrap gap-2">
                        {(["command", "button", "modal"] as const).map((type) => (
                          <button
                            key={type}
                            type="button"
                            onClick={() => {
                              const current = new Set(target.logTypes);
                              if (current.has(type)) {
                                current.delete(type);
                              } else {
                                current.add(type);
                              }
                              updateConsoleTarget(index, {
                                logTypes: Array.from(current)
                              });
                            }}
                            className={cn(
                              "rounded-md border px-3 py-1 text-xs font-semibold transition-colors",
                              target.logTypes.includes(type)
                                ? "border-primary bg-primary/15 text-primary"
                                : "border-border text-muted-foreground hover:bg-muted"
                            )}
                          >
                            {type}
                          </button>
                        ))}
                      </div>

                      <Button
                        className="mt-3"
                        variant="outline"
                        onClick={() => removeConsoleTarget(index)}
                        disabled={(coreDraft?.builtins.consoleTargets.length ?? 0) <= 1}
                      >
                        Remove Target
                      </Button>
                    </div>
                  ))}

                  <Button variant="outline" onClick={addConsoleTarget}>
                    Add Console Target
                  </Button>
                </CardContent>
                <CardFooter>
                  <Button onClick={() => void saveCore()} disabled={savingCore || loading || !dirtyCore}>
                    {savingCore ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />}
                    Save Core Changes
                  </Button>
                </CardFooter>
              </Card>
            </TabsContent>

            <TabsContent value="plugins">
              <div className="grid gap-4 2xl:grid-cols-[minmax(0,1.2fr)_minmax(420px,520px)]">
                <div className="min-w-0 space-y-4">
                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>Installed Plugins</CardTitle>
                      <CardDescription>有 `enabled` 旗標的插件會顯示即時開關。</CardDescription>
                    </CardHeader>
                    <CardContent className="grid gap-3 lg:grid-cols-2">
                      {togglePlugins.map((plugin) => {
                        const busy = Boolean(toggleBusy[plugin.name]);
                        return (
                          <div
                            key={plugin.name}
                            className={cn(
                              "min-w-0 rounded-lg border p-3 transition-colors",
                              selectedPlugin?.name === plugin.name
                                ? "border-primary bg-primary/10"
                                : "border-border hover:bg-muted/60"
                            )}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div className="min-w-0">
                                <button
                                  className="w-full truncate text-left font-semibold hover:text-primary"
                                  onClick={() => setSelectedPluginName(plugin.name)}
                                  title={plugin.name}
                                >
                                  {plugin.name}
                                </button>
                                <p className="text-xs text-muted-foreground">{plugin.category}</p>
                              </div>

                              <Switch
                                checked={plugin.enabled}
                                disabled={busy}
                                onCheckedChange={(checked) => {
                                  void togglePluginImmediate(plugin, checked);
                                }}
                              />
                            </div>

                            <p className="mt-3 text-xs text-muted-foreground">{plugin.description}</p>

                            <div className="mt-3 flex flex-wrap gap-1.5">
                              <Badge variant={plugin.enabled ? "success" : "secondary"}>
                                {plugin.enabled ? "Enabled" : "Disabled"}
                              </Badge>
                              <Badge variant={plugin.loaded ? "outline" : "secondary"}>
                                {plugin.loaded ? "Loaded" : "Not Loaded"}
                              </Badge>
                              {busy && <Badge variant="outline">Applying...</Badge>}
                            </div>
                          </div>
                        );
                      })}
                    </CardContent>
                  </Card>

                  {fixedPlugins.length > 0 && (
                    <Card className="bg-card/95 backdrop-blur">
                      <CardHeader>
                        <CardTitle>No Enabled Flag</CardTitle>
                        <CardDescription>這些插件沒有 `enabled` 欄位，僅可查看資訊與 YAML（若存在）。</CardDescription>
                      </CardHeader>
                      <CardContent className="grid gap-3 lg:grid-cols-2">
                        {fixedPlugins.map((plugin) => (
                          <div
                            key={plugin.name}
                            className={cn(
                              "min-w-0 rounded-lg border p-3 transition-colors",
                              selectedPlugin?.name === plugin.name
                                ? "border-primary bg-primary/10"
                                : "border-border hover:bg-muted/60"
                            )}
                          >
                            <button
                              className="w-full truncate text-left font-semibold hover:text-primary"
                              onClick={() => setSelectedPluginName(plugin.name)}
                              title={plugin.name}
                            >
                              {plugin.name}
                            </button>
                            <p className="mt-1 text-xs text-muted-foreground">{plugin.category}</p>
                            <p className="mt-2 text-xs text-muted-foreground">{plugin.description}</p>
                            <div className="mt-3 flex flex-wrap gap-1.5">
                              <Badge variant={plugin.loaded ? "outline" : "secondary"}>
                                {plugin.loaded ? "Loaded" : "Not Loaded"}
                              </Badge>
                              <Badge variant="warning">No enabled flag</Badge>
                            </div>
                          </div>
                        ))}
                      </CardContent>
                    </Card>
                  )}
                </div>

                <Card className="min-w-0 bg-card/95 backdrop-blur">
                  <CardHeader>
                    <CardTitle className="flex items-center gap-2">
                      {selectedPlugin ? <Puzzle className="size-4" /> : <CloudOff className="size-4" />}
                      {selectedPlugin?.name ?? "Select Plugin"}
                    </CardTitle>
                    <CardDescription>顯示 info.yaml 內容與插件設定檔編輯。</CardDescription>
                  </CardHeader>

                  <CardContent className="min-w-0 space-y-4">
                    {!selectedPlugin ? (
                      <p className="text-sm text-muted-foreground">從左側選擇一個插件以查看細節。</p>
                    ) : (
                      <>
                        <div className="grid gap-2 sm:grid-cols-2">
                          <div className="rounded-md border bg-muted/40 px-3 py-2 text-sm">
                            <p className="text-xs text-muted-foreground">Author</p>
                            <p className="mt-1 font-medium">{selectedPlugin.author ?? "Unknown"}</p>
                          </div>
                          <div className="rounded-md border bg-muted/40 px-3 py-2 text-sm">
                            <p className="text-xs text-muted-foreground">Version</p>
                            <p className="mt-1 font-medium">{selectedPlugin.version ?? "Unknown"}</p>
                          </div>
                        </div>

                        <div className="space-y-1 text-sm">
                          <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Description</p>
                          <p>{selectedPlugin.description}</p>
                        </div>

                        <Separator />

                        {renderTagList("requireIntents", selectedPlugin.requireIntents)}
                        {renderTagList("requireCacheFlags", selectedPlugin.requireCacheFlags)}
                        {renderTagList("requireMemberCachePolicies", selectedPlugin.requireMemberCachePolicies)}
                        {renderTagList("dependPlugins", selectedPlugin.dependPlugins)}
                        {renderTagList("softDependPlugins", selectedPlugin.softDependPlugins)}

                        {selectedPlugin.configPath && (
                          <div>
                            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">config.yaml path</p>
                            <p
                              className="mt-2 w-full truncate rounded-md bg-muted/70 px-2 py-1 font-mono text-[11px] text-muted-foreground"
                              title={selectedPlugin.configPath}
                            >
                              {selectedPlugin.configPath}
                            </p>
                          </div>
                        )}

                        {!selectedPlugin.hasWebEditor ? (
                          <div className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">
                            此插件沒有可編輯的 <code>config.yaml</code>。
                          </div>
                        ) : (
                          <div className="space-y-2 min-w-0">
                            <div className="flex items-center justify-between">
                              <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Plugin YAML</p>
                              {loadingYaml && <LoaderCircle className="size-4 animate-spin text-muted-foreground" />}
                            </div>

                            {pluginYaml?.path && (
                              <p
                                className="w-full truncate rounded-md bg-muted/70 px-2 py-1 font-mono text-[11px] text-muted-foreground"
                                title={pluginYaml.path}
                              >
                                {pluginYaml.path}
                              </p>
                            )}

                            <Textarea
                              className="min-h-[280px] font-mono text-xs"
                              value={pluginYamlDraft}
                              onChange={(event) => setPluginYamlDraft(event.target.value)}
                              disabled={loadingYaml || !pluginYaml}
                            />
                            <p className="text-xs text-muted-foreground">YAML 變更需按 Save，並會立刻 reload 當前插件。</p>
                          </div>
                        )}
                      </>
                    )}
                  </CardContent>

                  <CardFooter className="flex gap-2">
                    <Button
                      variant="outline"
                      className="w-full"
                      disabled={!selectedPlugin?.hasWebEditor || loadingYaml || savingYaml}
                      onClick={() => selectedPlugin && void loadPluginYaml(selectedPlugin.name)}
                    >
                      <FileCode2 className="size-4" />
                      Reload YAML
                    </Button>
                    <Button
                      className="w-full"
                      disabled={!selectedPlugin?.hasWebEditor || savingYaml || loadingYaml || !dirtyPluginYaml}
                      onClick={() => void savePluginYaml()}
                    >
                      {savingYaml ? <LoaderCircle className="size-4 animate-spin" /> : <Save className="size-4" />}
                      Save YAML
                    </Button>
                  </CardFooter>
                </Card>
              </div>
            </TabsContent>
          </Tabs>
        </main>
      </div>
    </div>
  );
}

export default App;
