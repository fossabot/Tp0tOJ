<template>
  <div class="content-col">
    <v-container fluid>
      <v-row>
        <v-spacer></v-spacer>
        <v-col cols="8">
          <v-data-table :headers="headers" :items="writeups">
            <template v-slot:item.download="{ item }">
              <v-btn
                text
                color="primary"
                :href="`/wp?userId=${item.userId}`"
                target="_blank"
                :download="`wp-${item.userId}-${nameFilter(item.name)}.zip`"
              >
                download
              </v-btn>
            </template>
          </v-data-table>
        </v-col>
        <v-spacer></v-spacer>
      </v-row>
      <v-btn
        fab
        color="primary"
        absolute
        right
        bottom
        href="/allwp"
        target="_blank"
      >
        <v-icon>download</v-icon>
      </v-btn>
      <v-snackbar v-model="hasInfo" right bottom :timeout="3000">
        {{ infoText }}
        <!-- <v-spacer></v-spacer> -->
        <template v-slot:action="{ attrs }">
          <v-btn icon>
            <v-icon v-bind="attrs" @click="hasInfo = false">close</v-icon>
          </v-btn>
        </template>
      </v-snackbar>
    </v-container>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import gql from "graphql-tag";
import { WriteUpInfo, WriteUpInfoResult } from "@/struct";

@Component
export default class Writeup extends Vue {
  private headers = [
    { text: "name", value: "name" },
    { text: "mail", value: "mail" },
    { text: "solved", value: "solved" },
    { text: "", value: "download" }
  ];

  private writeups: WriteUpInfo[] = [];

  private infoText: string = "";
  private hasInfo: boolean = false;

  async mounted() {
    await this.loadData();
  }

  nameFilter(name: string) {
    return name.replaceAll(
      /([^\p{L}\p{M}\p{N}\p{P}\p{S}]|[/\\'"`?*:@<>|])/gu,
      ""
    );
  }

  async loadData() {
    try {
      let res = await this.$apollo.query<WriteUpInfoResult, {}>({
        query: gql`
          query {
            writeUpInfos {
              message
              infos {
                userId
                name
                mail
                solved
              }
            }
          }
        `,
        fetchPolicy: "no-cache"
      });
      if (res.errors) throw res.errors.map(v => v.message).join(",");
      if (res.data!.writeUpInfos.message) throw res.data!.writeUpInfos.message;
      this.writeups = res.data!.writeUpInfos.infos;
    } catch (e) {
      this.infoText = e.toString();
      this.hasInfo = true;
    }
  }
}
</script>

<style lang="scss" scoped>
.content-col {
  height: calc(100vh - 96px);
  overflow-y: auto;
}
</style>
