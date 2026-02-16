import { type ReactElement, useEffect, useMemo, useState } from "react";
import {
  Activity,
  Cable,
  CheckCircle2,
  CloudOff,
  Database,
  FileCode2,
  Globe,
  LayoutDashboard,
  LoaderCircle,
  Puzzle,
  RefreshCcw,
  Save,
  Settings2,
  Shield,
  Sparkles,
  Terminal,
  Workflow
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

const pluginCategoryIcon: Record<string, ReactElement> = {
  Moderation: <Shield className="size-4" />,
  Utility: <Settings2 className="size-4" />,
  Logging: <Database className="size-4" />,
  Voice: <Workflow className="size-4" />,
  Economy: <Sparkles className="size-4" />,
  Community: <Puzzle className="size-4" />,
  Ops: <Activity className="size-4" />,
  Music: <Cable className="size-4" />,
  Automation: <RefreshCcw className="size-4" />,
  Support: <Terminal className="size-4" />,
  Plugin: <Puzzle className="size-4" />
};

function normalizeYaml(input: string): string {
  return input.replace(/\r\n/g, "\n").trim();
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

  const unsavedCount = Number(dirtyCore) + Number(dirtyPluginYaml);
  const enabledPluginCount = plugins.filter((it) => it.enabled).length;

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

  function updateStatusMessage(index: number, value: string) {
    setCoreDraft((current) => {
      if (!current) return current;
      const next = [...current.builtins.statusMessages];
      next[index] = value;
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
          statusMessages: [...current.builtins.statusMessages, "COMPETING;New Status;5000"]
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
          statusMessages: coreDraft.builtins.statusMessages
            .map((line) => line.trim())
            .filter((line) => line.length > 0)
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

  async function saveAll() {
    if (!dirtyCore && !dirtyPluginYaml) return;
    if (dirtyCore) {
      await saveCore();
    }
    if (dirtyPluginYaml) {
      await savePluginYaml();
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
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="font-heading text-sm uppercase tracking-[0.18em] text-muted-foreground">
                XsDiscordBotKt
              </p>
              <h1 className="font-heading text-2xl font-semibold md:text-3xl">
                Configuration Dashboard
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Live config editing with immediate plugin toggle apply.
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Badge variant={health?.status === "ok" ? "success" : "warning"}>
                <CheckCircle2 className="mr-1.5 size-3.5" />
                {health?.status === "ok" ? "Server Connected" : "Offline"}
              </Badge>
              <Badge variant="secondary">
                <Globe className="mr-1.5 size-3.5" />
                {health?.recommendedBaseUrl ?? "http://127.0.0.1:21100"}
              </Badge>
              <Button
                variant={unsavedCount > 0 ? "warning" : "default"}
                onClick={() => void saveAll()}
                disabled={
                  loading ||
                  savingCore ||
                  savingYaml ||
                  unsavedCount === 0
                }
              >
                <Save className="size-4" />
                Save Pending {unsavedCount > 0 ? `(${unsavedCount})` : ""}
              </Button>
            </div>
          </div>

          {message && (
            <div className="mt-4 rounded-lg border border-border bg-background/60 px-3 py-2 text-sm text-muted-foreground">
              {message}
            </div>
          )}
        </header>

        <main className="mt-6 grid gap-6 lg:grid-cols-[280px_1fr]">
          <aside className="space-y-4">
            <Card className="bg-card/90 backdrop-blur">
              <CardHeader>
                <CardTitle className="flex items-center gap-2 text-base">
                  <LayoutDashboard className="size-4" />
                  Workspace
                </CardTitle>
                <CardDescription>
                  Runtime snapshot and operation status
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 text-sm">
                <div className="flex items-center justify-between rounded-md bg-muted/60 px-3 py-2">
                  <span className="text-muted-foreground">Core mode</span>
                  <span className="font-semibold">{health?.mode ?? "loading"}</span>
                </div>
                <div className="flex items-center justify-between rounded-md bg-muted/60 px-3 py-2">
                  <span className="text-muted-foreground">Plugins enabled</span>
                  <span className="font-semibold">{enabledPluginCount}</span>
                </div>
                <div className="flex items-center justify-between rounded-md bg-muted/60 px-3 py-2">
                  <span className="text-muted-foreground">Pending saves</span>
                  <span className="font-semibold">{unsavedCount}</span>
                </div>
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => void refreshAll()}
                  disabled={loading || refreshing || savingCore || savingYaml}
                >
                  <RefreshCcw className={cn("size-4", (loading || refreshing) && "animate-spin")} />
                  Refresh Snapshot
                </Button>
              </CardContent>
            </Card>

            <Card className="bg-card/90 backdrop-blur">
              <CardHeader>
                <CardTitle className="text-base">Plugin Matrix</CardTitle>
                <CardDescription>Immediate toggle, no save button required</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2">
                {plugins.slice(0, 8).map((plugin) => (
                  <button
                    key={plugin.name}
                    type="button"
                    onClick={() => setSelectedPluginName(plugin.name)}
                    className={cn(
                      "flex w-full items-center justify-between rounded-md border px-3 py-2 text-left transition-colors",
                      selectedPlugin?.name === plugin.name
                        ? "border-primary bg-primary/10"
                        : "border-border hover:bg-muted/60"
                    )}
                  >
                    <span className="text-sm font-medium">{plugin.name}</span>
                    <Badge variant={plugin.enabled ? "success" : "secondary"}>
                      {plugin.enabled ? "ON" : "OFF"}
                    </Badge>
                  </button>
                ))}
              </CardContent>
            </Card>
          </aside>

          <section>
            <Tabs defaultValue="core" className="w-full">
              <TabsList className="grid w-full grid-cols-3">
                <TabsTrigger value="core">Core Settings</TabsTrigger>
                <TabsTrigger value="plugins">Plugin Controls</TabsTrigger>
                <TabsTrigger value="connection">Connection</TabsTrigger>
              </TabsList>

              <TabsContent value="core">
                <div className="grid gap-4 xl:grid-cols-2">
                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>General</CardTitle>
                      <CardDescription>Mapped to root `config.yaml`</CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Bot token (masked)</label>
                        <Input value={coreDraft?.general.tokenMasked ?? ""} disabled />
                        <p className="text-xs text-muted-foreground">
                          Token is intentionally masked. Dashboard does not edit token directly.
                        </p>
                      </div>
                    </CardContent>
                  </Card>

                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>Status Changer</CardTitle>
                      <CardDescription>
                        Format: <code>TYPE;message;durationMs</code>
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      {(coreDraft?.builtins.statusMessages ?? []).map((line, index) => (
                        <div key={`${line}-${index}`} className="flex gap-2">
                          <Input
                            value={line}
                            onChange={(event) => updateStatusMessage(index, event.target.value)}
                          />
                          <Button
                            variant="outline"
                            onClick={() => removeStatusMessage(index)}
                            disabled={(coreDraft?.builtins.statusMessages.length ?? 0) <= 1}
                          >
                            Remove
                          </Button>
                        </div>
                      ))}
                      <Button variant="outline" onClick={addStatusMessage}>
                        Add Status Line
                      </Button>
                    </CardContent>
                  </Card>
                </div>

                <Card className="mt-4 bg-card/95 backdrop-blur">
                  <CardHeader>
                    <CardTitle>Console Logger Targets</CardTitle>
                    <CardDescription>
                      Changes apply after clicking Save Core Section
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {(coreDraft?.builtins.consoleTargets ?? []).map((target, index) => (
                      <div key={`${target.guildId}-${target.channelId}-${index}`} className="rounded-lg border p-4">
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
                      Save Core Section
                    </Button>
                  </CardFooter>
                </Card>
              </TabsContent>

              <TabsContent value="plugins">
                <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>Installed Plugins</CardTitle>
                      <CardDescription>
                        Toggle applies immediately. YAML edits require Save.
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="grid gap-3 md:grid-cols-2">
                      {plugins.map((plugin) => {
                        const busy = Boolean(toggleBusy[plugin.name]);
                        return (
                          <div
                            key={plugin.name}
                            className={cn(
                              "rounded-lg border p-3 transition-colors",
                              selectedPlugin?.name === plugin.name
                                ? "border-primary bg-primary/10"
                                : "border-border hover:bg-muted/60"
                            )}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div>
                                <button
                                  className="font-semibold hover:text-primary"
                                  onClick={() => setSelectedPluginName(plugin.name)}
                                >
                                  {plugin.name}
                                </button>
                                <p className="text-xs text-muted-foreground">{plugin.category}</p>
                              </div>
                              <Switch
                                checked={plugin.enabled}
                                disabled={!plugin.canToggle || busy}
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
                              {!plugin.canToggle && <Badge variant="warning">No enabled flag</Badge>}
                              {busy && <Badge variant="outline">Applying...</Badge>}
                            </div>
                          </div>
                        );
                      })}
                    </CardContent>
                  </Card>

                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle className="flex items-center gap-2">
                        {selectedPlugin
                          ? (pluginCategoryIcon[selectedPlugin.category] ?? <Puzzle className="size-4" />)
                          : <CloudOff className="size-4" />}
                        {selectedPlugin?.name ?? "Select Plugin"}
                      </CardTitle>
                      <CardDescription>
                        Runtime dependencies, intents, and raw YAML editor
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {!selectedPlugin ? (
                        <p className="text-sm text-muted-foreground">
                          Select a plugin from the left list to inspect details.
                        </p>
                      ) : (
                        <>
                          <div className="space-y-1 text-sm">
                            <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Description</p>
                            <p>{selectedPlugin.description}</p>
                          </div>

                          <Separator />

                          <div>
                            <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                              Runtime Dependencies
                            </p>
                            <div className="mt-2 flex flex-wrap gap-1.5">
                              {(selectedPlugin.dependencies.length > 0
                                ? selectedPlugin.dependencies
                                : ["None"]).map((dep) => (
                                <Badge key={dep} variant="outline">
                                  {dep}
                                </Badge>
                              ))}
                            </div>
                          </div>

                          <div>
                            <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                              Required Intents
                            </p>
                            <div className="mt-2 flex flex-wrap gap-1.5">
                              {(selectedPlugin.intents.length > 0
                                ? selectedPlugin.intents
                                : ["None"]).map((intent) => (
                                <Badge key={intent} variant="secondary">
                                  {intent}
                                </Badge>
                              ))}
                            </div>
                          </div>

                          {!selectedPlugin.hasWebEditor ? (
                            <div className="rounded-md border border-dashed p-3 text-xs text-muted-foreground">
                              This plugin has no editable <code>config.yaml</code> file.
                            </div>
                          ) : (
                            <div className="space-y-2">
                              <div className="flex items-center justify-between">
                                <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                                  Plugin YAML
                                </p>
                                {loadingYaml && <LoaderCircle className="size-4 animate-spin text-muted-foreground" />}
                              </div>
                              {pluginYaml?.path && (
                                <p className="rounded-md bg-muted/70 px-2 py-1 font-mono text-[11px] text-muted-foreground">
                                  {pluginYaml.path}
                                </p>
                              )}
                              <Textarea
                                className="min-h-[240px] font-mono text-xs"
                                value={pluginYamlDraft}
                                onChange={(event) => setPluginYamlDraft(event.target.value)}
                                disabled={loadingYaml || !pluginYaml}
                              />
                              <p className="text-xs text-muted-foreground">
                                Save applies YAML immediately and reloads this plugin. Toggle is still instant via switch.
                              </p>
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

              <TabsContent value="connection">
                <div className="grid gap-4 xl:grid-cols-2">
                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>Recommended Transport</CardTitle>
                      <CardDescription>
                        For local admin, localhost binding is safer than mDNS discovery.
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-4 text-sm">
                      <div className="rounded-lg border border-success/40 bg-success/10 p-3">
                        <p className="font-semibold text-success">Use localhost by default</p>
                        <p className="mt-1 text-muted-foreground">
                          Bind Ktor to <code>127.0.0.1:21100</code> to avoid accidental external exposure.
                        </p>
                      </div>

                      <div className="space-y-2 rounded-lg border border-border p-3">
                        <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Runtime env</p>
                        <Textarea
                          readOnly
                          value={
                            "XSBOT_DASHBOARD_ENABLED=true\n" +
                            "XSBOT_DASHBOARD_HOST=127.0.0.1\n" +
                            "XSBOT_DASHBOARD_PORT=21100\n" +
                            "# Optional LAN mode later: 0.0.0.0"
                          }
                          className="min-h-[140px]"
                        />
                      </div>
                    </CardContent>
                  </Card>

                  <Card className="bg-card/95 backdrop-blur">
                    <CardHeader>
                      <CardTitle>Save Policy</CardTitle>
                      <CardDescription>
                        Which operations are instant and which require Save
                      </CardDescription>
                    </CardHeader>
                    <CardContent className="space-y-3 text-sm text-muted-foreground">
                      <div className="rounded-lg border border-border p-3">
                        <p className="font-semibold text-foreground">Immediate</p>
                        <p>Plugin ON/OFF switch sends request immediately and reloads the selected plugin.</p>
                      </div>
                      <div className="rounded-lg border border-border p-3">
                        <p className="font-semibold text-foreground">Save Required</p>
                        <p>Core settings and plugin YAML edits are multi-field operations and require explicit Save.</p>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              </TabsContent>
            </Tabs>
          </section>
        </main>
      </div>
    </div>
  );
}

export default App;
